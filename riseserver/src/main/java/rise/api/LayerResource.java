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
import rise.lib.config.RiseConfig;
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
import wasdi.jwasdilib.WasdiLib;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

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
			Layer oLayer = null;
			
			if (oMap.isDateFiltered()) {
				oLayer = oLayerRepository.getLayerByAreaMapTime(sAreaId, sMapId, (double) dDate);
			}
			else {
				oLayer = oLayerRepository.getLayerByAreaMap(sAreaId, sMapId);
			}

			if (oLayer != null) {
				LayerViewModel oLayerViewModel = (LayerViewModel) RiseViewModel.getFromEntity(LayerViewModel.class.getName(), oLayer);
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
			String sWorkspaceName=oLayer.getAreaId()+"|"+oLayer.getPluginId() +"|"+oLayer.getMapId();
			WasdiLib oWasdiLib=new WasdiLib();
			
			/*File oConfigFile = new File("/home/jihed/Desktop/config.properties");
			Properties oProp = new Properties();
			if (oConfigFile.exists()) {
				System.out.println("/home/jihed/Desktop/config.properties");
				InputStream oInputStream = new FileInputStream("/home/jihed/Desktop/config.properties");

	            if (oInputStream != null) {
	            	System.out.println("input steam works");
	                oProp.load(oInputStream);
	                Enumeration<String> aoProperties =  (Enumeration<String>) oProp.propertyNames();
	                

	                

	                while (aoProperties.hasMoreElements()) {
	                    String sKey = aoProperties.nextElement();
	                    System.out.println(sKey);
	                    System.out.println(oProp.getProperty(sKey));
	                }
	                
	            }
			}*/
			if(oWasdiLib.init(RiseConfig.Current.wasdiConfig.wasdiConfigProperties)) {
				oWasdiLib.openWorkspace(sWorkspaceName);
				String sLink=oWasdiLib.getPath(oLayer.getId()+".tif");
				if(!Utils.isNullOrEmpty(sLink)) {
					return Response.ok(sLink)
							.header("Content-Disposition", "attachment; filename=" + sLayerId + "." + sFormat).build();	
				}else {
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}	
			}else {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.downloadLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	

}
