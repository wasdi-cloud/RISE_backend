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

def main(oConfig, oInput):
    # --- Layer Info ---

    WMS_URL = oConfig["geoserver"]["address"] + "/rise/wms"
    LAYER_NAME = oInput["layerIds"][0]  # Assuming only one layer ID is provided
    CRS = "EPSG:4326"
    WIDTH = 512
    HEIGHT = 512

    sOutputPath = oInput["outputPath"]

    sBbox = oInput["bbox"]

    # --- Extract BBOX from WKT ---
    try:
        oGeometry = wkt_loads(sBbox)  # Parse WKT string
        min_lng, min_lat, max_lng, max_lat = oGeometry.bounds  # Extract bounds
        bbox = (min_lat, min_lng, max_lat, max_lng)  # Format as (min_lat, min_lng, max_lat, max_lng)
    except Exception as e:
        logging.error(f"Error parsing WKT string: {sBbox}, {e}")
        return

    # --- BBOX from your shape ---
    bbox_corrected = (
        bbox[1],  # min lng
        bbox[0],  # min lat
        bbox[3],  # max lng
        bbox[2]   # max lat
    )

    # --- Build WMS request ---
    params = {
        "service": "WMS",
        "version": "1.1.1",
        "request": "GetMap",
        "layers": LAYER_NAME,
        "bbox": ",".join(map(str, bbox_corrected)),
        "width": WIDTH,
        "height": HEIGHT,
        "srs": CRS,
        "format": "image/png",
        "transparent": "true"
    }
    # --- Full Layer BBox from user ---
    full_layer_bbox = (
        1.4998625311991,  # min lng
        13.000169158400022,  # min lat
        3.4998716797628027,  # max lng
        15.000178306963724  # max lat
    )

    # --- WMS request for full layer (lower res for speed) ---
    FULL_WIDTH, FULL_HEIGHT = 256, 256
    params_full = {
        "service": "WMS",
        "version": "1.1.1",
        "request": "GetMap",
        "layers": LAYER_NAME,
        "bbox": ",".join(map(str, full_layer_bbox)),
        "width": FULL_WIDTH,
        "height": FULL_HEIGHT,
        "srs": CRS,
        "format": "image/png",
        "transparent": "true"
    }

    response = requests.get(WMS_URL, params=params)
    response.raise_for_status()

    # --- Show image ---
    image = Image.open(BytesIO(response.content))
    plt.imshow(image)
    plt.title("Flood Layer Preview")
    plt.axis("off")
    plt.show()

    # Convert image to grayscale (if needed) then NumPy array
    image_gray = image.convert("RGBA").convert("L")
    image_array = np.array(image_gray)
    # Apply simple filter: values > 0
    filtered_mask = image_array > 50
    # --- Stats ---
    total_pixels = image_array.size
    affected_pixels = np.sum(filtered_mask)
    # print(f"Affected pixels (> 0): {affected_pixels}")
    percent_affected = (affected_pixels / total_pixels) * 100

    # Optional: show filtered result
    # plt.imshow(filtered_mask, cmap='gray')
    # plt.title("Filtered Pixels (value > 0)")
    # plt.axis("off")
    # plt.show()

    response_full = requests.get(WMS_URL, params=params_full)
    response_full.raise_for_status()

    # --- Convert to NumPy (grayscale) ---
    image_full = Image.open(BytesIO(response_full.content))
    image_full_gray = image_full.convert("RGBA").convert("L")
    image_full_array = np.array(image_full_gray)
    full_filtered_mask = image_full_array > 50

    # --- Compute full-layer stats ---
    total_full_pixels = image_full_array.size
    affected_full_pixels = np.sum(full_filtered_mask)

    # --- d.ii Report ---
    percent_affected_drawn = (affected_pixels / total_pixels) * 100
    percent_affected_full = (affected_pixels / total_full_pixels) * 100
    print("\n=== [d.i] The total number of pixel affected in the area drawn ===")
    # print(f"Total pixels in the drawn shape: {total_pixels}")
    print(f"Affected pixels: {affected_pixels}")
    # print(f"Percentage affected in respect to both the area selected: {percent_affected:.2f}%")
    print("\n=== [d.ii] Percentage Affected ===")
    print(f"Within drawn area: {percent_affected_drawn:.2f}%")
    print(f"In respect to full layer: {percent_affected_full:.2f}%")

    # --- d.iii: Histogram of selected area ---

    plt.figure(figsize=(8, 4))
    plt.hist(image_array.flatten(), bins=50, color='steelblue', edgecolor='black')
    plt.title("[d.iii] Histogram of Pixel Values in Drawn Area")
    plt.xlabel("Pixel Value")
    plt.ylabel("Frequency")
    plt.grid(True)
    plt.tight_layout()
    plt.show()

    # Use the whole image or only filtered values
    hist_data = image_array.flatten()
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
    min_lat, min_lng, max_lat, max_lng = bbox
    center_lat = (min_lat + max_lat) / 2

    pixel_height_deg = (max_lat - min_lat) / HEIGHT
    pixel_width_deg = (max_lng - min_lng) / WIDTH

    # --- Convert to meters ---
    meters_per_deg_lat = 111_320  # constant
    meters_per_deg_lng = 111_320 * cos(radians(center_lat))

    pixel_height_m = pixel_height_deg * meters_per_deg_lat
    pixel_width_m = pixel_width_deg * meters_per_deg_lng

    pixel_area_m2 = pixel_height_m * pixel_width_m

    # --- Compute total and affected area ---
    area_drawn_m2 = image_array.size * pixel_area_m2
    area_affected_m2 = np.sum(filtered_mask) * pixel_area_m2

    print("\n=== [d.iv] Area Estimation ===")
    print(f"Pixel size: {pixel_width_m:.2f}m x {pixel_height_m:.2f}m")
    print(f"Area of drawn shape: {area_drawn_m2 / 1e6:.2f} km²")
    print(f"Area affected (filtered pixels): {area_affected_m2 / 1e6:.2f} km²")

    # --- d.v:  export and download the area selected ---

    export_all_pixels = False  # True = export all pixels, False = only affected

    rows, cols = image_array.shape
    export_data = []

    for row in range(rows):
        for col in range(cols):
            val = image_array[row, col]
            if not export_all_pixels and val == 0:
                continue  # skip unfiltered

            # Convert row/col to lat/lng
            lat = max_lat - (row + 0.5) * pixel_height_deg
            lng = min_lng + (col + 0.5) * pixel_width_deg

            export_data.append((lat, lng, val))

    # --- Export to CSV ---
    export_df = pd.DataFrame(export_data, columns=["Latitude", "Longitude", "Pixel Value"])
    export_df.to_csv(sOutputPath+"selected_area_pixels.csv", index=False)
    print("Exported pixel values and coordinates to 'selected_area_pixels.csv'")

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

        logging.debug('__main__: WASDI GFS Data Provider ' + sOperation)

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