package rise.api;

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
import rise.lib.business.Layer;
import rise.lib.business.Map;
import rise.lib.business.User;
import rise.lib.data.AreaRepository;
import rise.lib.data.LayerRepository;
import rise.lib.data.MapRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.LayerViewModel;
import rise.lib.viewmodels.RiseViewModel;

@Path("layer")
public class LayerResource {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLayer(@HeaderParam("x-session-token") String sSessionId, @QueryParam("map_id") String sMapId, @QueryParam("area_id") String sAreaId, @QueryParam("date") long lDate) {
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
    		
    		LayerRepository oLayerRepository = new LayerRepository();
    		Layer oLayer = oLayerRepository.getLayerByAreaMapTime(sAreaId, sMapId, (double) lDate);
    		
    		if (oLayer != null) {
        		LayerViewModel oLayerViewModel = (LayerViewModel) RiseViewModel.getFromEntity(LayerViewModel.class.getName(), oLayer);
        		return Response.ok(oLayerViewModel).build();
        		
    		}
    		else {
    			return Response.status(Status.NO_CONTENT).build();
    		}
		}
		catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.getLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
	}

}
