package rise.api;

import java.io.File;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
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
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.LayerViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.stream.FileStreamingOutput;
import wasdi.jwasdilib.WasdiLib;

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
				oLayer = oLayerRepository.getLayerByAreaMapTime(sAreaId, sMapId, (double) dDate/1000.0);
			}
			else {
				oLayer = oLayerRepository.getLayerByAreaMap(sAreaId, sMapId);
			}

			if (oLayer != null) {

				if (oMap.getMaxAgeDays()>=0 && oMap.isDateFiltered()) {
					long lReference = Double.valueOf(dDate).longValue();
					long lDistance = Math.abs(lReference - oLayer.getReferenceDate().longValue()*1000l);
					long lMaxAge = oMap.getMaxAgeDays()*24l*60l*60l*1000l;

					if (lDistance>lMaxAge) {
						RiseLog.infoLog("LayerResource.getLayer: found a layer but is too old, discard it");
						oLayer = null;
					}
				}

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
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
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
			
			
			WasdiLib oWasdiLib = new WasdiLib();
			
			oWasdiLib.setUser(RiseConfig.Current.wasdiConfig.wasdiUser);
			oWasdiLib.setPassword(RiseConfig.Current.wasdiConfig.wasdiPassword);
			
			if (oWasdiLib.init()) {
				
				if (Utils.isNullOrEmpty(oLayer.getWorkspaceId())) {
					String sWorkspaceName = oLayer.getAreaId() + "|" + oLayer.getPluginId() + "|" + oLayer.getMapId();
					oWasdiLib.openWorkspace(sWorkspaceName);
				}
				else {
					oWasdiLib.openWorkspaceById(oLayer.getWorkspaceId());
				}
				
				String sLocalFilePath = oWasdiLib.getPath(oLayer.getId() + ".tif");
				
				if (!Utils.isNullOrEmpty(sLocalFilePath)) {
					
					// Ok send the file to the user
					File oFile = new File(sLocalFilePath);
					FileStreamingOutput oStream = new FileStreamingOutput(oFile);
					ResponseBuilder oResponseBuilder = Response.ok(oStream);
					String sFileName = oFile.getName();
					oResponseBuilder.header("Content-Disposition", "attachment; filename=" + sFileName);
					//oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
					oResponseBuilder.header("Access-Control-Expose-Headers", "Content-Disposition");

					return oResponseBuilder.build();
				} 
				else {
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}
			} 
			else {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.downloadLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
