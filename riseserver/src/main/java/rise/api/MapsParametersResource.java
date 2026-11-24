package rise.api;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.MapsParameters;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.AreaRepository;
import rise.lib.data.MapsParametersRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.MapsParametersViewModel;
import rise.lib.viewmodels.RiseViewModel;


@Path("maps_parameters")
public class MapsParametersResource {

	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("area_id") String sAreaId, @QueryParam("map_id") String sMapId, @QueryParam("plugin_id") String sRequestedPluginId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
	
			if (oUser == null) {
				RiseLog.warnLog("MapResource.getParameters: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
	
			if (Utils.isNullOrEmpty(sAreaId) || Utils.isNullOrEmpty(sMapId)) {
				RiseLog.warnLog("MapResource.getParameters: some parameters are null or empty");
				return Response.status(Status.BAD_REQUEST).build();
			}
	
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);
	
			if (oArea == null) {
				RiseLog.warnLog("MapResource.getParameters: Area with this id " + sAreaId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
	
			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("MapResource.getParameters: user cannot access area");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			// Resolve the map from the plugin configuration. Maps need not have a DB map record.
			ArrayList<String> asPlugins = oArea.getPlugins();
			
			if (asPlugins == null || asPlugins.size() == 0) {
				RiseLog.warnLog("MapResource.getParameters: no plugins found for area");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			String sPluginId = sRequestedPluginId;
			if (Utils.isNullOrEmpty(sPluginId)) {
				sPluginId = findPluginIdForMap(asPlugins, sMapId);
			} else if (!asPlugins.contains(sPluginId)) {
				RiseLog.warnLog("MapResource.getParameters: plugin " + sPluginId + " is not active for area " + sAreaId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (Utils.isNullOrEmpty(sPluginId)) {
				RiseLog.warnLog("MapResource.getParameters: map " + sMapId + " not found in active plugin configurations for area " + sAreaId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			JsonNode oMapJsonConfig = getPluginMapConfig(sPluginId, sMapId);
			if (oMapJsonConfig == null) {
				RiseLog.warnLog("MapResource.getParameters: map " + sMapId + " is not configured for plugin " + sPluginId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			// Saved area parameters override the plugin's default parameters.
			MapsParametersRepository oParametersRepository = new MapsParametersRepository();
			MapsParameters oMapParameters = oParametersRepository.getMostRecentParameters(sAreaId, sPluginId, sMapId);
			
			if (oMapParameters != null) {
				RiseLog.debugLog("MapResource.getParameters: map " + sMapId + " has some oveloaded parameter");
				MapsParametersViewModel oMapParameterViewModel = (MapsParametersViewModel) RiseViewModel.getFromEntity(MapsParametersViewModel.class.getName(), oMapParameters);
				return Response.ok(oMapParameterViewModel).build();
			}
			
			RiseLog.debugLog("MapResource.getParameters: no overloaded parameters found; returning plugin defaults");
			JsonNode oMapJsonParams = oMapJsonConfig.get("params");
			if (oMapJsonParams == null) {
				RiseLog.warnLog("MapResource.getParameters: no default parameters found for map " + sMapId + " in plugin " + sPluginId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			MapsParametersViewModel oMapsParametersVM = new MapsParametersViewModel();
			oMapsParametersVM.areaId = sAreaId;
			oMapsParametersVM.pluginId = sPluginId;
			oMapsParametersVM.mapId = sMapId;
			oMapsParametersVM.payload = oMapJsonParams.toString();
			
			return Response.ok(oMapsParametersVM).build();
			
		} catch (Exception oE) {
			RiseLog.errorLog("MapResource.getParameters. Exception " + oE.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response addParameters(@HeaderParam("x-session-token") String sSessionId, MapsParametersViewModel oMapParametersViewModel) {
		
		try {
			User oUser = Rise.getUserFromSession(sSessionId);
			
			if (oUser == null) {
				RiseLog.warnLog("MapResource.add: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (oMapParametersViewModel == null) {
				RiseLog.warnLog("MapResource.add: map parameter view model is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (Utils.isNullOrEmpty(oMapParametersViewModel.areaId) 
					|| Utils.isNullOrEmpty(oMapParametersViewModel.mapId) 
					|| Utils.isNullOrEmpty(oMapParametersViewModel.payload)) {
				RiseLog.warnLog("MapResource.add: some fiels in the view model are null or empty");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			String sAreaId = oMapParametersViewModel.areaId;
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);
	
			if (oArea == null) {
				RiseLog.warnLog("MapResource.add: Area with this id " + sAreaId + " not found");
				return Response.status(Status.NOT_FOUND).build();
			}
	
			if (!PermissionsUtils.canUserWriteArea(oArea, oUser)) {
				RiseLog.warnLog("MapResource.add: user cannot access area");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("MapResource.add: not an HQ level");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			String sMapId = oMapParametersViewModel.mapId;

			ArrayList<String> asActivePluginIds = oArea.getPlugins();
			if (asActivePluginIds == null || asActivePluginIds.isEmpty()) {
				RiseLog.warnLog("MapResource.add: no plugins found for area " + sAreaId);
				return Response.status(Status.NOT_FOUND).build();
			}

			String sPluginId = oMapParametersViewModel.pluginId;
			if (Utils.isNullOrEmpty(sPluginId)) {
				sPluginId = findPluginIdForMap(asActivePluginIds, sMapId);
			} else if (!asActivePluginIds.contains(sPluginId)) {
				RiseLog.warnLog("MapResource.add: plugin " + sPluginId + " is not active for area " + sAreaId);
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (Utils.isNullOrEmpty(sPluginId) || getPluginMapConfig(sPluginId, sMapId) == null) {
				RiseLog.warnLog("MapResource.add: map " + sMapId + " is not configured for an active plugin of area " + sAreaId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			MapsParametersRepository oMapsParametersRepo = new MapsParametersRepository();
			MapsParameters oMapParameter = oMapsParametersRepo.getMostRecentParameters(sAreaId, sPluginId, sMapId);
			if (oMapParameter != null) {
				oMapParameter.setPayload(oMapParametersViewModel.payload);
				oMapParameter.setLastModifyUserId(oUser.getUserId());
				oMapParameter.setLastModifyTimestamp(Instant.now().toEpochMilli());
				if (!oMapsParametersRepo.update(oMapParameter, oMapParameter.getId())) {
					RiseLog.warnLog("MapResource.add: existing map parameters were not updated");
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}
				return Response.ok().build();
			}
			
			String sMapParameterId = Utils.getRandomName();
			while (oMapsParametersRepo.get(sMapParameterId) != null) {
				sMapParameterId = Utils.getRandomName();
			}
			
			MapsParameters oMapParameters = new MapsParameters();
			oMapParameters.setId(sMapParameterId);
			oMapParameters.setAreaId(sAreaId);
			oMapParameters.setMapId(sMapId);
			oMapParameters.setPluginId(sPluginId);
			oMapParameters.setPayload(oMapParametersViewModel.payload);
			oMapParameters.setCreationUserId(oUser.getUserId());
			long lNow = Instant.now().toEpochMilli();
			oMapParameters.setCreationTimestamp(lNow);
			oMapParameters.setLastModifyTimestamp(lNow);
			oMapParameters.setLastModifyUserId(oUser.getUserId());
			
			if (Utils.isNullOrEmpty(oMapsParametersRepo.add(oMapParameters))) {
				RiseLog.warnLog("MapResource.add: map parameters where not stored");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		
			return Response.ok().build();
		
		} catch (Exception oE) {
			RiseLog.errorLog("MapResource.add: exception " + oE.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@HeaderParam("x-session-token") String sSessionId, MapsParametersViewModel oMapParametersViewModel) {
		
		try {
			User oUser = Rise.getUserFromSession(sSessionId);
			
			if (oUser == null) {
				RiseLog.warnLog("MapResource.update: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (oMapParametersViewModel == null) {
				RiseLog.warnLog("MapResource.update: map parameter view model is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (Utils.isNullOrEmpty(oMapParametersViewModel.id) || Utils.isNullOrEmpty(oMapParametersViewModel.payload)) {
				RiseLog.warnLog("MapResource.update: some fiels in the view model are null or empty");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			MapsParametersRepository oMapsParametersRepo = new MapsParametersRepository();
			MapsParameters oMapsParametersFromDb = (MapsParameters) oMapsParametersRepo.get(oMapParametersViewModel.id);
			
			if (oMapsParametersFromDb == null) {
				RiseLog.warnLog("MapResource.update: no maps parameters found for id " + oMapParametersViewModel.id);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			String sAreaId = oMapsParametersFromDb.getAreaId();
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);
			
			if (oArea == null) {
				RiseLog.warnLog("MapResource.update: area not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (!PermissionsUtils.canUserWriteArea(oArea, oUser)) {
				RiseLog.warnLog("MapResource.update: user cannot access area");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("MapResource.update: not an HQ level");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			oMapsParametersFromDb.setPayload(oMapParametersViewModel.payload);
			oMapsParametersFromDb.setLastModifyUserId(oUser.getUserId());
			oMapsParametersFromDb.setLastModifyTimestamp(Instant.now().toEpochMilli());
			
			
			if (!oMapsParametersRepo.update(oMapsParametersFromDb, oMapsParametersFromDb.getId())) {
				RiseLog.warnLog("MapResource.update: map parameters where not updated");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		
			return Response.ok().build();
		
		} catch (Exception oE) {
			RiseLog.errorLog("MapResource.update: exception " + oE.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("maps_parameters") String sMapsParametersId) {
		
		try {
			User oUser = Rise.getUserFromSession(sSessionId);
			
			if (oUser == null) {
				RiseLog.warnLog("MapResource.update: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (Utils.isNullOrEmpty(sMapsParametersId)) {
				RiseLog.warnLog("MapResource.update: maps parameters id is null or empty");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			MapsParametersRepository oMapsParametersRepo = new MapsParametersRepository();
			MapsParameters oMapsParametersFromDb = (MapsParameters) oMapsParametersRepo.get(sMapsParametersId);
			
			if (oMapsParametersFromDb == null) {
				RiseLog.warnLog("MapResource.update: no maps parameters found for id " + sMapsParametersId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			String sAreaId = oMapsParametersFromDb.getAreaId();
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);
			
			if (oArea == null) {
				RiseLog.warnLog("MapResource.update: area not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (!PermissionsUtils.canUserWriteArea(oArea, oUser)) {
				RiseLog.warnLog("MapResource.update: user cannot access area");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("MapResource.update: not an HQ level");
				return Response.status(Status.FORBIDDEN).build();
			}

			
			if (oMapsParametersRepo.delete(sMapsParametersId) <= 0 ) {
				RiseLog.warnLog("MapResource.update: map parameters where not deleted");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		
			return Response.ok().build();
		
		} catch (Exception oE) {
			RiseLog.errorLog("MapResource.update: exception " + oE.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
				
	}
	
	private String findPluginIdForMap(ArrayList<String> asPluginIds, String sMapId) throws Exception {
		for (String sPluginId : asPluginIds) {
			if (!Utils.isNullOrEmpty(sPluginId) && getPluginMapConfig(sPluginId, sMapId) != null) {
				return sPluginId;
			}
		}
		return null;
	}

	private JsonNode getPluginMapConfig(String sPluginId, String sMapId) throws Exception {
		if (Utils.isNullOrEmpty(sPluginId) || Utils.isNullOrEmpty(sMapId)
				|| RiseConfig.Current == null || RiseConfig.Current.paths == null
				|| Utils.isNullOrEmpty(RiseConfig.Current.paths.riseConfigPath)) {
			return null;
		}

		java.nio.file.Path oConfigPath = Paths.get(RiseConfig.Current.paths.riseConfigPath).getParent();
		if (oConfigPath == null) {
			return null;
		}

		File oPluginConfigFile = oConfigPath.resolve(sPluginId + ".json").toFile();
		if (!oPluginConfigFile.isFile()) {
			return null;
		}

		JsonNode oMaps = new ObjectMapper().readTree(oPluginConfigFile).get("maps");
		if (oMaps == null || !oMaps.isArray()) {
			return null;
		}

		Iterator<JsonNode> oMapsIterator = oMaps.elements();
		while (oMapsIterator.hasNext()) {
			JsonNode oMapConfig = oMapsIterator.next();
			JsonNode oMapId = oMapConfig.get("id");
			if (oMapId != null && sMapId.equals(oMapId.asText())) {
				return oMapConfig;
			}
		}
		return null;
	}

}
