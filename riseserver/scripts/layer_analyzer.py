import json
import logging
import sys
from math import cos, radians

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import rasterio
import requests
import wasdi
from lxml import etree
from rasterio.windows import from_bounds
from shapely.wkt import loads as wkt_loads


def getLayerBbox(sWmsUrl, sLayerName):
    # Get the bounding box of the layer using WMS capabilities
    params = {
        "service": "WMS",
        "version": "1.3.0",
        "request": "GetCapabilities"
    }
    response = requests.get(sWmsUrl, params=params)
    response.raise_for_status()

    # Parse XML response
    tree = etree.fromstring(response.content)

    # Find the BoundingBox for the specific layer
    namespace = {"wms": "http://www.opengis.net/wms"}
    bounding_box = tree.xpath(f"//wms:Layer[wms:Name='{sLayerName}']/wms:BoundingBox", namespaces=namespace)

    # Extract bounding box coordinates
    if bounding_box:
        bbox = bounding_box[0]
        minx, miny = bbox.get("minx"), bbox.get("miny")
        maxx, maxy = bbox.get("maxx"), bbox.get("maxy")
        print(f"Bounding Box for {sLayerName}: ({minx}, {miny}, {maxx}, {maxy})")
        return (float(minx), float(miny), float(maxx), float(maxy))
    else:
        return None


def main(oConfig, oInput):
    oOutput = {}


    # --- User Creds ---
    sUserName = oConfig["wasdiConfig"]["wasdiUser"]
    sUserPassword = oConfig["wasdiConfig"]["wasdiPassword"]

    # --- Layer Info ---

    sLayerName = oInput["layerIds"][0]  # Assuming only one layer ID is provided
    sAreaId = oInput["areaId"]
    sPluginId = oInput["pluginId"]
    sMapId = oInput["mapId"]
    # Writing folder
    sOutputPath = oInput["outputPath"]
    # Bbox to analyze
    sBbox = oInput["bbox"]
    # Filter value (optional)
    fFilter = None
    if "filter" in oInput:
        fFilter = oInput["filter"]

    # # --- Extract BBOX of the Target Area from WKT ---
    # sBbox = "POLYGON ((49.689789 -15.496032, 49.839478 -15.496032, 49.839478 -15.406024, 49.689789 -15.406024, 49.689789 -15.496032))"  # Example simple square bbox
    try:
        # Parse WKT string
        oGeometry = wkt_loads(sBbox)
        # Extract bounds
        fMinLng, fMinLat, fMaxLng, fMaxLat = oGeometry.bounds
        # Format as (min_lat, min_lng, max_lat, max_lng)
        oTargetAreaBbox = (fMinLat, fMinLng, fMaxLat, fMaxLng)
    except Exception as e:
        logging.error(f"Error parsing WKT string: {sBbox}, {e}")
        return

    # # --- BBOX from the user shape ---
    oFinalTargetAreaBbox = (
        oTargetAreaBbox[1],  # min lng
        oTargetAreaBbox[0],  # min lat
        oTargetAreaBbox[3],  # max lng
        oTargetAreaBbox[2]  # max lat
    )



    wasdi.setUser(sUserName)
    wasdi.setPassword(sUserPassword)
    if not wasdi.init():
        print("Initialization failed")
        return

    sWorkspaceName = sAreaId + "|" + sPluginId + "|" + sMapId
    wasdi.openWorkspace(sWorkspaceName)
    sGeoTiffFilePath = wasdi.getPath(sLayerName + ".tif")
    if not sGeoTiffFilePath:
        logging.error(f"Could not get GeoTIFF path for layer: {sLayerName}")
        return

    # ======================================================================
    # Main Processing Block: Reading GeoTIFF data and performing calculations
    # ======================================================================
    try:
        with rasterio.open(sGeoTiffFilePath) as src:
            iNoData = src.nodata
            pCRS = src.crs  # Coordinate Reference System for area calculation
            dPixelWidth_native, dPixelHeight_native = src.res  # Native pixel resolution for area calculation

            # ======================================================================
            # Part 1: Calculations for the Drawn Area (Target Area)
            # ======================================================================

            # --- Read data for the Target Area (drawn shape) ---
            window_drawn_area = from_bounds(oFinalTargetAreaBbox[0], oFinalTargetAreaBbox[1],
                                            oFinalTargetAreaBbox[2], oFinalTargetAreaBbox[3],
                                            src.transform)

            afTargetAreaImageArray = src.read(1, window=window_drawn_area)
            transform_target_area = src.window_transform(window_drawn_area)  # Transform specific to this window
            # Get the actual bounds of the data that was read for the drawn area (for precise area calculation)
            fMinLng_actual_drawn, fMinLat_actual_drawn, fMaxLng_actual_drawn, fMaxLat_actual_drawn = src.window_bounds(
                window_drawn_area)

            # 1. Handle NoData values: Convert them to NaN for robust filtering and counting
            if iNoData is not None:
                afTargetAreaImageArray = np.where(afTargetAreaImageArray == iNoData, np.nan, afTargetAreaImageArray)

            # 2. Create afFilteredMask based on fFilter
            if fFilter is not None:
                afFilteredMask = (afTargetAreaImageArray > fFilter) & (~np.isnan(afTargetAreaImageArray))
            else:
                afFilteredMask = ~np.isnan(afTargetAreaImageArray)

            # 3. Calculate iTargetAreaTotalPixels
            iTargetAreaTotalPixels = np.count_nonzero(~np.isnan(afTargetAreaImageArray))

            # 4. Calculate iAffectedPixels (for the drawn area)
            iAffectedPixels = np.sum(afFilteredMask)

            # 5. Calculate fPercentAffected (for the drawn area)
            fPercentAffected = (iAffectedPixels / iTargetAreaTotalPixels) * 100 if iTargetAreaTotalPixels > 0 else 0

            print(f"\n--- Results for the Drawn Area (Maronsetra Bbox) ---")
            print(f"Shape of afTargetAreaImageArray (pixels within bbox): {afTargetAreaImageArray.shape}")
            print(f"Filter value used: {fFilter}")
            print(f"Total Pixels in Drawn Area (excluding NoData): {iTargetAreaTotalPixels}")
            print(f"Affected Pixels in Drawn Area (based on filter): {iAffectedPixels}")
            print(f"Percentage Affected in Drawn Area: {fPercentAffected:.2f}%")

            # ======================================================================
            # Part 2: Calculations for the Full Layer
            # ======================================================================

            # --- Read data for the Full Layer (afImageFullArray) ---
            afImageFullArray = src.read(1)

            # Handle NoData values in the full image array
            if iNoData is not None:
                afImageFullArray = np.where(afImageFullArray == iNoData, np.nan, afImageFullArray)

            # Create afFullFilteredMask based on fFilter
            if fFilter is not None:
                afFullFilteredMask = (afImageFullArray > fFilter) & (~np.isnan(afImageFullArray))
            else:
                afFullFilteredMask = ~np.isnan(afImageFullArray)

            # --- Compute full-layer stats ---
            iTotalFullPixels = np.count_nonzero(~np.isnan(afImageFullArray))
            iAffectedFullPixels = np.sum(afFullFilteredMask)

            print(f"\n--- Results for the Full Layer ---")
            print(f"Shape of afImageFullArray: {afImageFullArray.shape}")
            print(f"Total Pixels in Full Layer (excluding NoData): {iTotalFullPixels}")
            print(f"Affected Pixels in Full Layer (based on filter): {iAffectedFullPixels}")

            # ======================================================================
            # Part 3: d.ii Report Percentages
            # ======================================================================

            percent_affected_drawn = fPercentAffected
            percent_affected_full = (iAffectedPixels / iTotalFullPixels) * 100 if iTotalFullPixels > 0 else 0

            oOutput["totAreaPixels"] = str(iTotalFullPixels)
            oOutput["areaPixelAffected"] = str(iAffectedPixels)
            oOutput["percentTotAreaAffectedPixels"] = str(percent_affected_full)
            oOutput["percentAreaAffectedPixels"] = str(percent_affected_drawn)

            print(f"\n=== d.ii Report Percentages ===")
            print(f"Percentage Affected Within Drawn Area: {percent_affected_drawn:.2f}%")
            print(f"Percentage Affected In Respect to Full Layer: {percent_affected_full:.2f}%")

            # ======================================================================
            # Part 4: d.iii: Histogram of selected area
            # ======================================================================
            print("\n=== [d.iii] Histogram Generation ===")
            # Use only valid data for histogram
            aiHistogramData = afTargetAreaImageArray[~np.isnan(afTargetAreaImageArray)].flatten()

            plt.figure(figsize=(8, 4))
            plt.hist(aiHistogramData, bins=50, color='steelblue', edgecolor='black')
            plt.title("[d.iii] Histogram of Pixel Values in Drawn Area")
            plt.xlabel("Pixel Value")
            plt.ylabel("Frequency")
            plt.grid(True)
            plt.tight_layout()
            # plt.show() # Uncomment if you are running this interactively and want to see the plot
            plt.savefig(sOutputPath + "histogram_output.png")  # Save the plot to file
            plt.close()  # Close the plot to free memory

            # Convert histogram data to string list for output
            oOutput["histogram"] = [str(int(val)) for val in aiHistogramData if not np.isnan(val)]

            # Compute histogram data for export (hist, bin_edges)
            hist, bin_edges = np.histogram(aiHistogramData, bins=50)

            hist_df = pd.DataFrame({
                "Pixel Value Range Start": bin_edges[:-1],
                "Pixel Value Range End": bin_edges[1:],
                "Frequency": hist
            })

            hist_df.to_csv(sOutputPath + "histogram_output.csv", index=False)
            print("Histogram saved as 'histogram_output.csv'")
            hist_df.to_excel(sOutputPath + "histogram_output.xlsx", index=False)
            print("Histogram also saved as 'histogram_output.xlsx'")

            # ======================================================================
            # Part 5: d.iv: The estimation of the area both drawn and with pixel affected
            # ======================================================================
            print("\n=== [d.iv] Area Estimation ===")
            # Use the actual bounds of the drawn area as read from GeoTIFF for more precision
            fCenterLat = (fMinLat_actual_drawn + fMaxLat_actual_drawn) / 2

            fPixelWidth = dPixelWidth_native  # Already in native CRS units (e.g., degrees or meters)
            fPixelHeight = dPixelHeight_native  # Already in native CRS units (e.g., degrees or meters)

            iPixelWidthM = 0
            iPixelHeightM = 0

            if pCRS.is_geographic:  # Check if CRS is geographic (like WGS84 - EPSG:4326)
                # If geographic, convert degrees to meters
                iMetersPerDegLat = 111_320  # constant approximation at equator
                iMetersPerDegLng = 111_320 * cos(radians(fCenterLat))

                iPixelHeightM = fPixelHeight * iMetersPerDegLat
                iPixelWidthM = fPixelWidth * iMetersPerDegLng
            elif pCRS.is_projected:  # If CRS is projected (like UTM), dimensions are already in meters
                iPixelHeightM = fPixelHeight
                iPixelWidthM = fPixelWidth
            else:
                logging.warning(
                    f"CRS {pCRS.to_string()} is neither geographic nor projected. Area calculation might be inaccurate.")
                iPixelHeightM = fPixelHeight  # Fallback, assume it's in meters or a generic unit
                iPixelWidthM = fPixelWidth

            iPixelAreaM2 = iPixelHeightM * iPixelWidthM

            fAreaDrawnM2 = iTargetAreaTotalPixels * iPixelAreaM2
            fAreaAffectedM2 = iAffectedPixels * iPixelAreaM2  # Use iAffectedPixels from the drawn area

            print(f"Pixel size: {iPixelWidthM:.2f}m x {iPixelHeightM:.2f}m")
            print(f"Area of drawn shape: {fAreaDrawnM2 / 1e6:.2f} square km ")
            print(f"Area affected (filtered pixels): {fAreaAffectedM2 / 1e6:.2f} square km")

            oOutput["totAreaPixels"] = str(iTotalFullPixels)
            oOutput["areaPixelAffected"] = str(iAffectedPixels)
            oOutput["percentTotAreaAffectedPixels"] = str(percent_affected_full)
            oOutput["percentAreaAffectedPixels"] = str(percent_affected_drawn)
            oOutput["estimatedArea"] = str(fAreaDrawnM2 / 1e6)
            oOutput["estimatedAffectedArea"] = str(fAreaAffectedM2 / 1e6)

            # ======================================================================
            # Part 6: d.v: Export and download the area selected
            # ======================================================================
            print("\n=== [d.v] Pixel Export ===")
            bExportAllPixels = False  # True = export all pixels, False = only affected

            iRows, iCols = afTargetAreaImageArray.shape
            aoExport_data = []

            for iRow in range(iRows):
                for iCol in range(iCols):
                    fVal = afTargetAreaImageArray[iRow, iCol]

                    # Skip if it's nodata or if not exporting all pixels and value is 0 (or some other irrelevant value)
                    if np.isnan(fVal) or (not bExportAllPixels and fVal == 0):  # Assuming 0 might mean "not affected"
                        continue

                    # Convert row/col to lat/lng using the transform specific to the *read* target area
                    fLng, fLat = transform_target_area * (iCol + 0.5, iRow + 0.5)  # +0.5 for pixel center

                    aoExport_data.append((fLat, fLng, fVal))

            oExportedDataFrame = pd.DataFrame(aoExport_data, columns=["Latitude", "Longitude", "Pixel Value"])
            oExportedDataFrame.to_csv(sOutputPath + "selected_area_pixels.csv", index=False)
            print("Exported pixel values and coordinates to 'selected_area_pixels.csv'")

            return oOutput  # Return the final output dictionary

    except rasterio.errors.RasterioIOError as e:
        logging.error(f"Rasterio error reading GeoTIFF: {e}")
        return oOutput
    except Exception as e:
        logging.error(f"General error processing GeoTIFF: {e}")
        return oOutput



def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""


def readFileAsJsonObject(sFilePath):
    if stringIsNullOrEmpty(sFilePath):
        logging.warning(f'readFileAsJsonObject: file path is None or empty string: {sFilePath}')
        return None

    aoReturnObject = None
    try:
        with open(sFilePath) as oWasdiConfigJsonFile:
            aoReturnObject = json.load(oWasdiConfigJsonFile)
    except Exception as oEx:
        logging.warning(f'readFileAsJsonObject: error reading the file: {sFilePath}, {oEx}')
        return None

    if aoReturnObject is None:
        logging.warning(f'readFileAsJsonObject:  file is empty: {sFilePath}')
        return None

    return aoReturnObject


if __name__ == "__main__":
    # let's read the arguments
    asArgs = sys.argv

    try:
        if asArgs is None or len(asArgs) < 5:
            logging.error("__main__: no arguments passed to the data provider")
            sys.exit(1)

        sOperation = asArgs[1]
        sInputFile = asArgs[2]
        sOutputFile = asArgs[3]
        sRiseConfigFile = asArgs[4]

        print('__main__: RISE Layer Analyzer ' + sOperation)

        # first argument asArgs[0] is the name of the file - we are not interested in it
        print('__main__: operation ' + sOperation)
        print('__main__: input file ' + sInputFile)
        print('__main__: output file: ' + sOutputFile)
        print('__main__: wasdi config path: ' + sRiseConfigFile)

        oConfig = readFileAsJsonObject(sRiseConfigFile)
        oInputFile = readFileAsJsonObject(sInputFile)

        oOutput = main(oConfig, oInputFile)

        if oOutput is None:
            print("__main__: no output")
            sys.exit(1)

        if not stringIsNullOrEmpty(sOutputFile):
            with open(sOutputFile, 'w') as oOutputFile:
                json.dump(oOutput, oOutputFile, indent=4)
                print('__main__: output file: ' + sOutputFile)
        else:
            print("__main__: output file is None or Empty")
            sys.exit(1)

    except Exception as oE:
        print('__main__: Exception ' + str(oE))
        sys.exit(1)
