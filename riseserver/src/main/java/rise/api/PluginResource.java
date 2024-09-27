package rise.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Plugin;
import rise.lib.business.User;
import rise.lib.data.PluginRepository;
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
}
