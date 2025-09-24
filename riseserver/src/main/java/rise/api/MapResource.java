package rise.api;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.Map;
import rise.lib.business.MapsParameters;
import rise.lib.business.Plugin;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.AreaRepository;
import rise.lib.data.MapRepository;
import rise.lib.data.MapsParametersRepository;
import rise.lib.data.PluginRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.AreaViewModel;
import rise.lib.viewmodels.MapsParametersViewModel;
import rise.lib.viewmodels.MapViewModel;
import rise.lib.viewmodels.RiseViewModel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Path("map")
public class MapResource {

	@GET
	@Path("by_area")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMapsForArea(@HeaderParam("x-session-token") String sSessionId, @QueryParam("area_id") String sAreaId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("MapResource.getMapsForArea: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		if (Utils.isNullOrEmpty(sAreaId)) {
				RiseLog.warnLog("MapResource.getMapsForArea: Area id null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}    		
			
    		// Check if we have this subscription
    		AreaRepository oAreaRepository = new AreaRepository();
    		Area oArea = (Area) oAreaRepository.get(sAreaId);
			
    		if (oArea == null) {
				RiseLog.warnLog("MapResource.getMapsForArea: Area with this id " + sAreaId + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("MapResource.getMapsForArea: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();     			
    		}
    		
    		PluginRepository oPluginRepository = new PluginRepository();
    		ArrayList<String> asMapIds = new ArrayList<>();
    		
    		for (String sPluginId : oArea.getPlugins()) {
    			Plugin oPlugin = (Plugin) oPluginRepository.get(sPluginId);
				
    			for (String sMapId : oPlugin.getMaps()) {
    				if (Utils.isNullOrEmpty(sMapId)) continue;
					if (!asMapIds.contains(sMapId)) asMapIds.add(sMapId);
				}
			}
    		
    		ArrayList<MapViewModel> aoMapViewModels = new ArrayList<>();
    		MapRepository oMapRepository = new MapRepository();
    		
    		for (String sMapId : asMapIds) {
				Map oMap = (Map) oMapRepository.get(sMapId);
				
				if (oMap == null) {
					RiseLog.warnLog("MapResource.getMapsForArea: map not found " + sMapId);
					continue;
				}
				
				if (oMap.isHidden()) {
					RiseLog.debugLog("MapResource.getMapsForArea: map hidden: " + sMapId);
					continue;					
				}
				
				MapViewModel oMapViewModel = (MapViewModel) RiseViewModel.getFromEntity(MapViewModel.class.getName(), oMap);
				aoMapViewModels.add(oMapViewModel);
			}
    		
    		return Response.ok(aoMapViewModels).build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("MapResource.getMapsForArea: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
	}

	@GET
	@Path("parameters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("area_id") String sAreaId, @QueryParam("map_id") String sMapId) {
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
				return Response.status(Status.NOT_FOUND).build();
			}
	
			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("MapResource.getParameters: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Check if we have this map id
			MapRepository oMapRepository = new MapRepository();
			Map oMap = (Map) oMapRepository.get(sMapId);
			
			if (oMap == null) {
				RiseLog.warnLog("MapResource.getParameters: Map with id " + sMapId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// get the active plugins of the area
			ArrayList<String> asPlugins = oArea.getPlugins();
			
			if (asPlugins == null || asPlugins.size() == 0) {
				RiseLog.warnLog("MapResource.getParameters: no plugins found for area");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			// find the pluging containing that map
			PluginRepository oPluginRepository = new PluginRepository();
			String sPluginId = null;
			
			for (String sId : asPlugins) {
				if (oPluginRepository.hasMap(sId, sMapId)) {
					sPluginId = sId;
					break;
				}
			}
			
			if (Utils.isNullOrEmpty(sPluginId)) {
				RiseLog.warnLog("MapResource.getParameters: map " + sMapId + " not found among active plugins of area " + sAreaId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			ObjectMapper oMapper = new ObjectMapper();
			
			// first try to get the area from some parameters stored in the db
			MapsParametersRepository oParametersRepository = new MapsParametersRepository();
			MapsParameters oMapParameters = oParametersRepository.getMostRecentParameters(sAreaId, sPluginId, sMapId);
			
			if (oMapParameters != null) {
				RiseLog.debugLog("MapResource.getParameters: map " + sMapId + " has some oveloaded parameter");
				MapsParametersViewModel oMapParameterViewModel = (MapsParametersViewModel) RiseViewModel.getFromEntity(MapsParametersViewModel.class.getName(), oMapParameters);
				return Response.ok(oMapParameterViewModel).build();
			}
			
			RiseLog.debugLog("MapResource.getParameters: no overloaded parameters found. Will proceed with the default ones");
			String sPluginConfigFileName = sPluginId + ".json";
			java.nio.file.Path oPath = Paths.get(RiseConfig.Current.paths.riseConfigPath).getParent();
			
			if (oPath == null) {
				RiseLog.warnLog("MapResource.getParameters: base folder of Rise not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			String sRiseBaseFolder = oPath.toString();
			
			if (!sRiseBaseFolder.endsWith(File.separator)) sRiseBaseFolder += File.separator;
			
			String sPluginFilePath = sRiseBaseFolder + sPluginConfigFileName;
			
			File oFile = new File(sPluginFilePath);
			
			if (!oFile.exists()) {
				RiseLog.warnLog("MapResource.getParameters: the plugin configuration file " + sPluginFilePath + " does not exist");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			
			JsonNode oJson = oMapper.readTree(oFile);
			JsonNode oJsonMaps = oJson.get("maps");
			
			if (oJsonMaps == null) {
				RiseLog.warnLog("MapResource.getParameters: 'maps' entry not found in configuration of plugin " + sPluginId);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			Iterator<JsonNode> oMapsIterator = oJsonMaps.elements();
			JsonNode oMapJsonConfig = null;
			while (oMapsIterator.hasNext()) {
				JsonNode oJsonMap = oMapsIterator.next();
				JsonNode oJsonId = oJsonMap.get("id");
				if (oJsonId == null) continue;
				if (oJsonId.asText().equals(sMapId)) {
					oMapJsonConfig = oJsonMap;
					break;
				}
			}
			
			if (oMapJsonConfig == null) {
				RiseLog.warnLog("MapResource.getParameters. No configuration for map " + sMapId + " found in plugin config file " + sPluginFilePath);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			JsonNode oMapJsonParams = oMapJsonConfig.get("params");
			if (oMapJsonParams == null) {
				RiseLog.warnLog("MapResource.getParameters. No parameters found for map "+ sMapId + " found in plugin config file " + sPluginFilePath);
				return Response.status(Status.NOT_FOUND).build();
			}
			
			MapsParametersViewModel oMapsParametersVM = new MapsParametersViewModel();
			oMapsParametersVM.areaId = sAreaId;
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
	public Response add(@HeaderParam("x-session-token") String sSessionId, MapsParametersViewModel oMapParametersViewModel) {
		
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
	
			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("MapResource.add: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Check if we have this map id
			String sMapId = oMapParametersViewModel.mapId;
			MapRepository oMapRepository = new MapRepository();
			Map oMap = (Map) oMapRepository.get(sMapId);
			
			if (oMap == null) {
				RiseLog.warnLog("MapResource.add: Map with id " + sMapId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			
			//TODO: which other rights does the user need to access an area?
			// does they need to be HQ or field operators? Do they need valid subscriptions
			
			PluginRepository oPluginRepository = new PluginRepository();
			Plugin oPlugin = oPluginRepository.getPluginFromMapId(sMapId);
			
			if (oPlugin == null) {
				RiseLog.warnLog("MapResource.add: plugin for map " + sMapId + " not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			String sMapParameterId = Utils.getRandomName();
			MapsParametersRepository oMapsParametersRepo = new MapsParametersRepository();
			while (oMapsParametersRepo.get(sMapParameterId) != null) {
				sMapParameterId = Utils.getRandomName();
			}
			
			MapsParameters oMapParameters = new MapsParameters();
			oMapParameters.setId(sMapParameterId);
			oMapParameters.setAreaId(sAreaId);
			oMapParameters.setMapId(sMapId);
			oMapParameters.setPluginId(oPlugin.getId());
			oMapParameters.setPayload(oMapParametersViewModel.payload);
			oMapParameters.setUserId(oUser.getUserId());
			oMapParameters.setCreationTimestamp(Instant.now().toEpochMilli());
			
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

}
