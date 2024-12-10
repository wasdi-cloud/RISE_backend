package rise.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.Layer;
import rise.lib.business.Map;
import rise.lib.business.User;
import rise.lib.data.AreaRepository;
import rise.lib.data.LayerRepository;
import rise.lib.data.MapRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.http.HttpCallResponse;
import rise.lib.utils.http.HttpUtils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.LayerViewModel;
import rise.lib.viewmodels.RiseViewModel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Path("layer")
public class LayerResource {

	@GET
	@Path("find")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLayer(@HeaderParam("x-session-token") String sSessionId, @QueryParam("map_id") String sMapId,
			@QueryParam("area_id") String sAreaId, @QueryParam("date") Long oDate) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("LayerResource.getLayer: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sAreaId)) {
				RiseLog.warnLog("LayerResource.getLayer: Area id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Check if we have this subscription
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);

			if (oArea == null) {
				RiseLog.warnLog("LayerResource.getLayer: Area with this id " + sAreaId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("LayerResource.getLayer: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sMapId)) {
				RiseLog.warnLog("LayerResource.getLayer: Map id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			MapRepository oMapRepository = new MapRepository();
			Map oMap = (Map) oMapRepository.get(sMapId);

			if (oMap == null) {
				RiseLog.warnLog("LayerResource.getLayer: Map with this id " + sMapId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (oDate == null)
				oDate = 0L;

			double dDate = (double) oDate;

			if (dDate <= 0.0)
				dDate = DateUtils.getNowAsDouble();

			LayerRepository oLayerRepository = new LayerRepository();
			Layer oLayer = oLayerRepository.getLayerByAreaMapTime(sAreaId, sMapId, (double) dDate);

			if (oLayer != null) {
				LayerViewModel oLayerViewModel = (LayerViewModel) RiseViewModel
						.getFromEntity(LayerViewModel.class.getName(), oLayer);
				return Response.ok(oLayerViewModel).build();

			} else {
				return Response.status(Status.NO_CONTENT).build();
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.getLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("download_layer")
	public Response downloadLayer(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("layer_id") String sLayerId, @QueryParam("format") String sFormat) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("LayerResource.downloadLayer: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sLayerId)) {
				RiseLog.warnLog("LayerResource.downloadLayer: layer id null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (Utils.isNullOrEmpty(sFormat)) {
				RiseLog.warnLog("LayerResource.downloadLayer: format null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (!sFormat.equals("shp") && !sFormat.equals("geotiff")) {
				RiseLog.warnLog("LayerResource.downloadLayer: invalid format ");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// verify layer exist
			LayerRepository oLayerRepository = new LayerRepository();
			Layer oLayer = (Layer) oLayerRepository.get(sLayerId);
			if (oLayer == null) {
				RiseLog.warnLog("LayerResource.downloadLayer: layer null");
				return Response.status(Status.NOT_FOUND).build();
			}
			// get geoserver url
			String sGeoserverUrl = oLayer.getGeoserverUrl();

			// Build the appropriate request URL based on WMS or WFS
			String sUrl = buildRequestUrl(sLayerId, sFormat, sGeoserverUrl);

			// Send the request to GeoServer and get the response
			byte[] aoResponseBytes = sendRequestToGeoServer(sUrl);

			// Return the response to the client as a file download
			return Response.ok(aoResponseBytes)
					.header("Content-Disposition", "attachment; filename=" + sLayerId + "." + sFormat).build();

		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.downloadLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private String buildRequestUrl(String sLayerId, String sFormat, String sGeoserverUrl) {
	    StringBuilder sUrl = new StringBuilder(sGeoserverUrl);
	    try {
	        if ("geotiff".equalsIgnoreCase(sFormat)) {
	            String bbox = getBoundingBox(sGeoserverUrl, sLayerId); // Fetch bbox dynamically
	        	//String bbox = "1.499846321040724,13.000077232365719,3.4998554696044266,15.000086380929421";
	            sUrl.append("service=WMS");
	            sUrl.append("&version=1.1.1");
	            sUrl.append("&request=GetMap");
	            sUrl.append("&layers=rise:").append(sLayerId);
	            sUrl.append("&bbox=").append(bbox); // Use the dynamic bbox
	            sUrl.append("&width=256");
	            sUrl.append("&height=256");
	            sUrl.append("&srs=EPSG:4326");
	            sUrl.append("&format=image/").append(sFormat);
	            //sUrl.append("&transparent=true");
	        } else if ("shp".equalsIgnoreCase(sFormat)) {
	            sUrl.append("?service=WFS");
	            sUrl.append("&version=2.0.0");
	            sUrl.append("&request=GetFeature");
	            sUrl.append("&typeName=rise:").append(sLayerId);
	            sUrl.append("&outputFormat=").append(sFormat);
	        } else {
	            throw new IllegalArgumentException("Unsupported format: " + sFormat);
	        }
	    } catch (Exception e) {
	        throw new RuntimeException("Error building request URL: " + e.getMessage(), e);
	    }
	    return sUrl.toString();
	}


	private byte[] sendRequestToGeoServer(String url) {
		HttpCallResponse oResponse = HttpUtils.httpGet(url);

		
		if (oResponse.getResponseCode() >= 200 && oResponse.getResponseCode() <= 299) {
			return oResponse.getResponseBytes();
		} else {
			String sErrorMessage = "GeoServer request failed with response code: " + oResponse.getResponseCode()
					+ " and message: " + oResponse.getResponseBody();
			throw new RuntimeException(sErrorMessage);
		}
	}
	public  String getBoundingBox(String sGeoserverUrl, String sLayerId) throws Exception {
        // Construct the GetCapabilities URL
        String sCapabilitiesUrl = sGeoserverUrl + "service=WMS&version=1.1.1&request=GetCapabilities";
        
        // Fetch the capabilities document
        DocumentBuilderFactory oFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder oBuilder = oFactory.newDocumentBuilder();
        Document oDoc = oBuilder.parse(sCapabilitiesUrl);
        
        // Find the Layer element for the specified layer ID
        NodeList oLayers = oDoc.getElementsByTagName("Layer");
        for (int i = 0; i < oLayers.getLength(); i++) {
            Element oLayerElement = (Element) oLayers.item(i);
            NodeList oNameElements = oLayerElement.getElementsByTagName("Name");
            if (oNameElements.getLength() > 0 && oNameElements.item(0).getTextContent().equals(sLayerId)) {
                Element oBBoxElement = (Element) oLayerElement.getElementsByTagName("LatLonBoundingBox").item(0);
                if (oBBoxElement != null) {
                    String sMinx = oBBoxElement.getAttribute("minx");
                    String sMiny = oBBoxElement.getAttribute("miny");
                    String sMaxx = oBBoxElement.getAttribute("maxx");
                    String sMaxy = oBBoxElement.getAttribute("maxy");
                    return sMinx + "," + sMiny + "," + sMaxx + "," + sMaxy;
                }
            }
        }
        throw new Exception("Layer not found or bounding box missing in capabilities document");
    }
	

}
