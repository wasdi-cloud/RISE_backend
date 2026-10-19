import json
import logging
import os
import sys
from math import cos, radians

import numpy as np
import pandas as pd
import rasterio
import requests
import wasdi
from lxml import etree
from rasterio.mask import mask
from rasterio.transform import xy
from rasterio.warp import transform, transform_geom
from shapely.geometry import mapping
from shapely.wkt import loads as wkt_loads


def getLayerBbox(sWmsUrl, sLayerName):
    params = {"service": "WMS", "version": "1.3.0", "request": "GetCapabilities"}
    response = requests.get(sWmsUrl, params=params)
    response.raise_for_status()
    tree = etree.fromstring(response.content)
    namespace = {"wms": "http://www.opengis.net/wms"}
    bounding_box = tree.xpath(
        f"//wms:Layer[wms:Name='{sLayerName}']/wms:BoundingBox",
        namespaces=namespace,
    )
    if not bounding_box:
        return None
    bbox = bounding_box[0]
    return tuple(float(bbox.get(key)) for key in ("minx", "miny", "maxx", "maxy"))


def _write_exports(output_path, selected, selected_transform, source_crs):
    values = selected.compressed()
    histogram_bins = min(50, max(1, np.unique(values).size)) if values.size else 1
    histogram, bin_edges = np.histogram(values, bins=histogram_bins)
    histogram_frame = pd.DataFrame(
        {
            "Pixel Value Range Start": bin_edges[:-1],
            "Pixel Value Range End": bin_edges[1:],
            "Frequency": histogram,
        }
    )

    histogram_csv = os.path.join(output_path, "histogram_output.csv")
    histogram_excel = os.path.join(output_path, "histogram_output.xlsx")
    histogram_frame.to_csv(histogram_csv, index=False)
    histogram_frame.to_excel(histogram_excel, index=False)

    rows, cols = selected.shape
    row_indices, col_indices = np.indices((rows, cols))
    x_coordinates, y_coordinates = xy(
        selected_transform, row_indices, col_indices, offset="center"
    )
    flat_x = np.asarray(x_coordinates).ravel().tolist()
    flat_y = np.asarray(y_coordinates).ravel().tolist()
    longitudes, latitudes = transform(source_crs, "EPSG:4326", flat_x, flat_y)
    longitudes = np.asarray(longitudes).reshape((rows, cols))
    latitudes = np.asarray(latitudes).reshape((rows, cols))
    included = ~selected.mask
    pixels_frame = pd.DataFrame(
        {
            "Longitude": longitudes[included],
            "Latitude": latitudes[included],
            "Pixel Value": selected.data[included],
        }
    )
    pixels_csv = os.path.join(output_path, "selected_area_pixels.csv")
    pixels_frame.to_csv(pixels_csv, index=False)

    return {
        "histogram": [str(value) for value in values.tolist()],
        "histogramCsv": histogram_csv,
        "histogramExcel": histogram_excel,
        "selectedPixelsCsv": pixels_csv,
    }


def main(oConfig, oInput):
    try:
        username = oConfig["wasdiConfig"]["wasdiUser"]
        password = oConfig["wasdiConfig"]["wasdiPassword"]
        layer_name = oInput["layerIds"][0]
        output_path = oInput["outputPath"]
        if not output_path:
            raise ValueError("outputPath is required")
        os.makedirs(output_path, exist_ok=True)

        geometry = wkt_loads(oInput["bbox"])
        if geometry.is_empty or not geometry.is_valid:
            raise ValueError("bbox must contain a valid, non-empty WKT geometry")

        filter_value = oInput.get("filter")
        pixel_filter = None if filter_value in (None, "") else float(filter_value)

        wasdi.setUser(username)
        wasdi.setPassword(password)
        if not wasdi.init():
            raise RuntimeError("WASDI initialization failed")
        workspace_name = (
            oInput["areaId"] + "|" + oInput["pluginId"] + "|" + oInput["mapId"]
        )
        wasdi.openWorkspace(workspace_name)
        geotiff_path = wasdi.getPath(layer_name + ".tif")
        if not geotiff_path:
            raise RuntimeError(f"Could not get GeoTIFF path for layer: {layer_name}")

        with rasterio.open(geotiff_path) as source:
            if source.crs is None:
                raise ValueError("The GeoTIFF has no CRS")

            raster_geometry = transform_geom(
                "EPSG:4326", source.crs, mapping(geometry)
            )
            selected, selected_transform = mask(
                source, [raster_geometry], crop=True, filled=False
            )
            selected = selected[0]
            full = source.read(1, masked=True)
            selected_values = selected.compressed()
            full_values = full.compressed()

            total_selected = int(selected_values.size)
            total_full = int(full_values.size)
            if pixel_filter is None:
                affected_values = selected_values
                affected_full_values = full_values
            else:
                affected_values = selected_values[selected_values > pixel_filter]
                affected_full_values = full_values[full_values > pixel_filter]

            affected_selected = int(affected_values.size)
            affected_full = int(affected_full_values.size)
            percent_selected = (
                affected_selected / total_selected * 100 if total_selected else 0
            )
            percent_full = (
                affected_selected / total_full * 100 if total_full else 0
            )

            exports = _write_exports(
                output_path, selected, selected_transform, source.crs
            )

            pixel_width, pixel_height = (abs(value) for value in source.res)
            if source.crs.is_geographic:
                center_latitude = geometry.centroid.y
                pixel_width_m = pixel_width * 111320 * cos(radians(center_latitude))
                pixel_height_m = pixel_height * 111320
            elif source.crs.is_projected:
                unit_factor = source.crs.linear_units_factor[1]
                pixel_width_m = pixel_width * unit_factor
                pixel_height_m = pixel_height * unit_factor
            else:
                raise ValueError(f"Unsupported CRS for area estimation: {source.crs}")

            pixel_area_m2 = pixel_width_m * pixel_height_m
            result = {
                "totAreaPixels": str(total_full),
                "areaPixelAffected": str(affected_selected),
                "percentTotAreaAffectedPixels": str(percent_full),
                "percentAreaAffectedPixels": str(percent_selected),
                "estimatedArea": str(total_selected * pixel_area_m2 / 1e6),
                "estimatedAffectedArea": str(affected_selected * pixel_area_m2 / 1e6),
                "affectedFullLayerPixels": str(affected_full),
            }
            result.update(exports)
            return result
    except rasterio.errors.RasterioIOError:
        logging.exception("Rasterio error processing layer")
    except (KeyError, TypeError, ValueError, RuntimeError):
        logging.exception("Invalid layer analyzer input or processing failure")
    except Exception:
        logging.exception("Unexpected layer analyzer failure")
    return None


def stringIsNullOrEmpty(value):
    return value is None or value == ""


def readFileAsJsonObject(file_path):
    if stringIsNullOrEmpty(file_path):
        logging.warning("JSON file path is empty")
        return None
    try:
        with open(file_path) as json_file:
            return json.load(json_file)
    except (OSError, ValueError):
        logging.exception("Could not read JSON file: %s", file_path)
        return None


if __name__ == "__main__":
    if len(sys.argv) < 5:
        logging.error("Expected operation, input file, output file and config file")
        sys.exit(1)

    operation, input_file, output_file, config_file = sys.argv[1:5]
    logging.info("RISE Layer Analyzer %s", operation)
    config = readFileAsJsonObject(config_file)
    input_object = readFileAsJsonObject(input_file)
    if config is None or input_object is None:
        sys.exit(1)

    output = main(config, input_object)
    if output is None or stringIsNullOrEmpty(output_file):
        sys.exit(1)
    with open(output_file, "w") as output_json:
        json.dump(output, output_json, indent=4)
