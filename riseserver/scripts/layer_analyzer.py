import json
import logging
import sys
import requests
import matplotlib.pyplot as plt
from io import BytesIO
from PIL import Image
import numpy as np
from math import cos, radians
import pandas as pd
from shapely.wkt import loads as wkt_loads
from lxml import etree

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

    # --- Layer Info ---
    sWMSUrl = oConfig["geoserver"]["address"] + "/rise/wms"
    sLayerName = oInput["layerIds"][0]  # Assuming only one layer ID is provided
    oCRS = "EPSG:4326"
    iWidth = 512
    iHeight = 512

    # Writing folder
    sOutputPath = oInput["outputPath"]
    # Bbox to analyze
    sBbox = oInput["bbox"]

    # Filter value (optional)
    fFilter = None
    if "filter" in oInput:
        fFilter = oInput["filter"]

    # --- Extract BBOX of the Target Area from WKT ---
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

    # --- BBOX from the user shape ---
    oFinalTargetAreaBbox = (
        oTargetAreaBbox[1],  # min lng
        oTargetAreaBbox[0],  # min lat
        oTargetAreaBbox[3],  # max lng
        oTargetAreaBbox[2]   # max lat
    )

    # --- Build WMS request for target area ---
    oTargetAreaWMSRequest = {
        "service": "WMS",
        "version": "1.1.1",
        "request": "GetMap",
        "layers": sLayerName,
        "bbox": ",".join(map(str, oFinalTargetAreaBbox)),
        "width": iWidth,
        "height": iHeight,
        "srs": oCRS,
        "format": "image/png",
        "transparent": "true"
    }


    # --- Full Layer BBox  ---
    oFullLayerBbox = getLayerBbox(sWMSUrl, sLayerName)

    # --- WMS request for full layer (lower res for speed) ---
    oWMSParamsFullLayer = {
        "service": "WMS",
        "version": "1.1.1",
        "request": "GetMap",
        "layers": sLayerName,
        "bbox": ",".join(map(str, oFullLayerBbox)),
        "width": iWidth,
        "height": iHeight,
        "srs": oCRS,
        "format": "image/png",
        "transparent": "true"
    }

    oResponse = requests.get(sWMSUrl, params=oTargetAreaWMSRequest)
    oResponse.raise_for_status()

    # --- Open image ---
    oTargetAreaImage = Image.open(BytesIO(oResponse.content))

    #plt.imshow(image)
    #plt.title("Flood Layer Preview")
    #plt.axis("off")
    #plt.show()

    # Convert image to grayscale (if needed) then NumPy array
    oTargetAreaImageGray = oTargetAreaImage.convert("RGBA").convert("L")
    afTargetAreaImageArray = np.array(oTargetAreaImageGray)

    if fFilter:
        afFilteredMask = afTargetAreaImageArray > fFilter
    else:
        # No filter applied, use all pixels
        afFilteredMask = afTargetAreaImageArray

    # --- Stats ---
    iTargetAreaTotalPixels = afTargetAreaImageArray.size
    iAffectedPixels = np.sum(afFilteredMask)
    fPercentAffected = (iAffectedPixels / iTargetAreaTotalPixels) * 100

    oResponseFullLayer = requests.get(sWMSUrl, params=oWMSParamsFullLayer)
    oResponseFullLayer.raise_for_status()

    # --- Convert to NumPy (grayscale) ---
    oImageFull = Image.open(BytesIO(oResponseFullLayer.content))
    oImageFullGray = oImageFull.convert("RGBA").convert("L")
    afImageFullArray = np.array(oImageFullGray)

    if fFilter:
        afFullFilteredMask = afImageFullArray > fFilter
    else:
        # No filter applied, use all pixels
        afFullFilteredMask = afImageFullArray

    # --- Compute full-layer stats ---
    iTotalFullPixels = afImageFullArray.size
    iAffectedFullPixels = np.sum(afFullFilteredMask)

    # --- d.ii Report ---
    percent_affected_drawn = (iAffectedPixels / iTargetAreaTotalPixels) * 100
    percent_affected_full = (iAffectedPixels / iTotalFullPixels) * 100

    oOutput["totAreaPixels"] = iTotalFullPixels
    
    oOutput["areaPixelAffected"] = iAffectedPixels
    oOutput["percentTotAreaAffectedPixels"] = percent_affected_full
    oOutput["percentAreaAffectedPixels"] = percent_affected_drawn

    print("\n=== [d.i] The total number of pixel affected in the area drawn ===")
    # print(f"Total pixels in the drawn shape: {total_pixels}")
    print(f"Affected pixels: {iAffectedPixels}")
    # print(f"Percentage affected in respect to both the area selected: {percent_affected:.2f}%")
    print("\n=== [d.ii] Percentage Affected ===")
    print(f"Within drawn area: {percent_affected_drawn:.2f}%")
    print(f"In respect to full layer: {percent_affected_full:.2f}%")

    # --- d.iii: Histogram of selected area ---

    plt.figure(figsize=(8, 4))
    plt.hist(afTargetAreaImageArray.flatten(), bins=50, color='steelblue', edgecolor='black')
    plt.title("[d.iii] Histogram of Pixel Values in Drawn Area")
    plt.xlabel("Pixel Value")
    plt.ylabel("Frequency")
    plt.grid(True)
    plt.tight_layout()
    plt.show()

    # Use the whole image or only filtered values
    hist_data = afTargetAreaImageArray.flatten()
    # Optional: uncomment below to export only affected (non-zero) pixels
    # hist_data = image_array[filtered_mask].flatten()

    # --- Compute histogram ---
    hist, bin_edges = np.histogram(hist_data, bins=50)

    # Build DataFrame for export
    hist_df = pd.DataFrame({
        "Pixel Value Range Start": bin_edges[:-1],
        "Pixel Value Range End": bin_edges[1:],
        "Frequency": hist
    })

    # --- Export to CSV ---
    hist_df.to_csv(sOutputPath+"histogram_output.csv", index=False)
    print("\nHistogram saved as 'histogram_output.csv'")

    # --- Optional: Export to Excel ---
    hist_df.to_excel(sOutputPath+"histogram_output.xlsx", index=False)
    print("Histogram also saved as 'histogram_output.xlsx'")

    # --- d.iv: The estimation of the area both drawn and with pixel affected ---
    # --- Get lat/lng resolution ---
    fMinLat, fMinLng, fMaxLat, fMaxLng = oTargetAreaBbox
    fCenterLat = (fMinLat + fMaxLat) / 2

    fPixelHeightDeg = (fMaxLat - fMinLat) / iHeight
    fPixelWidthDeg = (fMaxLng - fMinLng) / iWidth

    # --- Convert to meters ---
    iMetersPerDegLat = 111_320  # constant
    iMetersPerDegLng = 111_320 * cos(radians(fCenterLat))

    iPixelHeightM = fPixelHeightDeg * iMetersPerDegLat
    iPixelWidthM = fPixelWidthDeg * iMetersPerDegLng

    iPixelAreaM2 = iPixelHeightM * iPixelWidthM

    # --- Compute total and affected area ---
    fAreaDrawnM2 = afTargetAreaImageArray.size * iPixelAreaM2
    fAreaAffectedM2 = np.sum(afFilteredMask) * iPixelAreaM2

    print("\n=== [d.iv] Area Estimation ===")
    print(f"Pixel size: {iPixelWidthM:.2f}m x {iPixelHeightM:.2f}m")
    print(f"Area of drawn shape: {fAreaDrawnM2 / 1e6:.2f} km²")
    print(f"Area affected (filtered pixels): {fAreaAffectedM2 / 1e6:.2f} km²")
    oOutput["estimatedArea"] = fAreaDrawnM2 / 1e6
    oOutput["estimatedAffectedArea"] = fAreaAffectedM2 / 1e6

    # --- d.v:  export and download the area selected ---

    bExportAllPixels = False  # True = export all pixels, False = only affected

    iRows, iCols = afTargetAreaImageArray.shape
    aoExport_data = []

    for iRow in range(iRows):
        for iCol in range(iCols):
            fVal = afTargetAreaImageArray[iRow, iCol]
            if not bExportAllPixels and fVal == 0:
                continue  # skip unfiltered

            # Convert row/col to lat/lng
            fLat = fMaxLat - (iRow + 0.5) * fPixelHeightDeg
            fLng = fMinLng + (iCol + 0.5) * fPixelWidthDeg

            aoExport_data.append((fLat, fLng, fVal))

    # --- Export to CSV ---
    oExportedDataFrame = pd.DataFrame(aoExport_data, columns=["Latitude", "Longitude", "Pixel Value"])
    oExportedDataFrame.to_csv(sOutputPath+"selected_area_pixels.csv", index=False)
    print("Exported pixel values and coordinates to 'selected_area_pixels.csv'")

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

        logging.debug('__main__: RISE Layer Analyzer ' + sOperation)

        # first argument asArgs[0] is the name of the file - we are not interested in it
        logging.debug('__main__: operation ' + sOperation)
        logging.debug('__main__: input file ' + sInputFile)
        logging.debug('__main__: output file: ' + sOutputFile)
        logging.debug('__main__: wasdi config path: ' + sRiseConfigFile)

        oConfig = readFileAsJsonObject(sRiseConfigFile)
        oInputFile = readFileAsJsonObject(sInputFile)

        oOutput = main(oConfig, oInputFile)

        if oOutput is None:
            logging.error("__main__: no output")
            sys.exit(1)    
        
        if not stringIsNullOrEmpty(sOutputFile):
            with open(sOutputFile, 'w') as oOutputFile:
                json.dump(oOutput, oOutputFile, indent=4)
                logging.debug('__main__: output file: ' + sOutputFile)
        else: 
            logging.error("__main__: output file is None or Empty")
            sys.exit(1)    

    except Exception as oE:
        logging.error('__main__: Exception ' + str(oE))
        sys.exit(1)