import requests
import matplotlib.pyplot as plt
from io import BytesIO
from PIL import Image
import numpy as np
from math import cos, radians
import pandas as pd

def main():
    # --- Layer Info ---
    WMS_URL = "https://test.wasdi.net/geoserver/rise/wms"
    LAYER_NAME = "rise:fca18f7f74c048768a8efc3259407a60viirsflood_2025-04-09_flooded"
    CRS = "EPSG:4326"
    WIDTH = 512
    HEIGHT = 512

    # --- BBOX from your shape ---
    bbox = (
        -17.07253857905758,  # min lat
        47.79153557507427,  # min lng
        -14.083301314706778,  # max lat
        50.899877141324055  # max lng
    )

    # Note: WMS uses bbox as (minx, miny, maxx, maxy) => (min_lng, min_lat, max_lng, max_lat)
    # So we need to swap lat/lng for WMS correctly
    bbox_corrected = (
        bbox[1],  # min lng
        bbox[0],  # min lat
        bbox[3],  # max lng
        bbox[2]  # max lat
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
    hist_df.to_csv("histogram_output.csv", index=False)
    print("\n✅ Histogram saved as 'histogram_output.csv'")

    # --- Optional: Export to Excel ---
    hist_df.to_excel("histogram_output.xlsx", index=False)
    print("✅ Histogram also saved as 'histogram_output.xlsx'")

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
    export_df.to_csv("selected_area_pixels.csv", index=False)
    print("✅ Exported pixel values and coordinates to 'selected_area_pixels.csv'")


if __name__ == "__main__":
    main()