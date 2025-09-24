package rise.api;

import java.util.ArrayList;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.Map;
import rise.lib.business.Plugin;
import rise.lib.business.User;
import rise.lib.data.AreaRepository;
import rise.lib.data.MapRepository;
import rise.lib.data.PluginRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.MapViewModel;
import rise.lib.viewmodels.RiseViewModel;

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
	@Path("by_plugin")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMapsByPlugin(@HeaderParam("x-session-token") String sSessionId, @QueryParam("plugin_id") String sPluginId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

    		if (oUser == null) {
				RiseLog.warnLog("MapResource.getMapsByPlugin: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
    		}

    		if (Utils.isNullOrEmpty(sPluginId)) {
				RiseLog.warnLog("MapResource.getMapsByPlugin: Plugin id null");
				return Response.status(Status.BAD_REQUEST).build();
    		}

    		PluginRepository oPluginRepository = new PluginRepository();
    		ArrayList<String> asMapIds = new ArrayList<>();

    		for (Plugin oPlugin : oPluginRepository.getAll()) {
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
					RiseLog.warnLog("MapResource.getMapsByPlugin: map not found " + sMapId);
					continue;
				}

				if (oMap.isHidden()) {
					RiseLog.debugLog("MapResource.getMapsByPlugin: map hidden: " + sMapId);
					continue;
				}

				MapViewModel oMapViewModel = (MapViewModel) RiseViewModel.getFromEntity(MapViewModel.class.getName(), oMap);
				aoMapViewModels.add(oMapViewModel);
			}

    		return Response.ok(aoMapViewModels).build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("MapResource.getMapsByPlugin: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}
