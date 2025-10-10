package rise.api;

import java.util.ArrayList;
import java.util.List;

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
import rise.lib.business.Plugin;
import rise.lib.business.User;
import rise.lib.data.AreaRepository;
import rise.lib.data.PluginRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.PluginListViewModel;
import rise.lib.viewmodels.RiseViewModel;

@Path("plugins")
public class PluginResource {
	
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getList(@HeaderParam("x-session-token") String sSessionId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("PluginResource.getList: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		PluginRepository oPluginRepository = new PluginRepository();
    		List<Plugin> aoPlugins = oPluginRepository.getAll();
    		
    		ArrayList<PluginListViewModel> aoPluginViewModels = new ArrayList<>();
    		
    		for (Plugin oPlugin : aoPlugins) {
    			PluginListViewModel oPluginListViewModel = (PluginListViewModel) RiseViewModel.getFromEntity(PluginListViewModel.class.getName(), oPlugin);
    			aoPluginViewModels.add(oPluginListViewModel);
			}
    		
    		return Response.ok(aoPluginViewModels).build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("PluginResource.getList: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}    		
	}
	
	@GET
	@Path("by_area")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getListByArea(@HeaderParam("x-session-token") String sSessionId, @QueryParam("area_id") String sAreaId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("PluginResource.getListByArea: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		if (Utils.isNullOrEmpty(sAreaId)) {
    			RiseLog.warnLog("PluginResource.getListByArea: empty area id");
    			return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);
	
			if (oArea == null) {
				RiseLog.warnLog("PluginResource.getListByArea: Area with this id " + sAreaId + " not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("PluginResource.getListByArea: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}
    		
    		// get the active plugins of the area
			ArrayList<String> asPlugins = oArea.getPlugins();
			
			if (asPlugins == null || asPlugins.size() == 0) {
				RiseLog.warnLog("PluginResource.getListByArea: no plugins found for area");
				return Response.status(Status.NOT_FOUND).build();
			}
    					
    		PluginRepository oPluginRepository = new PluginRepository();
    		
    		List<Plugin> aoPlugins = oPluginRepository.listById(asPlugins);
    		
    		if (aoPlugins == null || aoPlugins.size() == 0) {
    			RiseLog.warnLog("PluginResource.getListByArea: no plugins found");
    			return Response.status(Status.NOT_FOUND).build();
    		}
    		
    		List<PluginListViewModel> aoPluginViewModels = new ArrayList<>();
    		
    		for (Plugin oPlugin : aoPlugins) {
    			PluginListViewModel oPluginViewModel = (PluginListViewModel) RiseViewModel.getFromEntity(PluginListViewModel.class.getName(), oPlugin);
    			aoPluginViewModels.add(oPluginViewModel);
			}
    		
    		return Response.ok(aoPluginViewModels).build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("PluginResource.getListByArea: ", oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}    		
	}
}
