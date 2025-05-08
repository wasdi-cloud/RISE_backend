package rise.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.Organization;
import rise.lib.business.User;
import rise.lib.business.WidgetInfo;
import rise.lib.data.AreaRepository;
import rise.lib.data.OrganizationRepository;
import rise.lib.data.WidgetInfoRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.WidgetInfoViewModel;

@Path("widget")
public class WidgetResource {
	
	@GET
	@Path("bydate")
	public Response getWidgetByTime(@HeaderParam("x-session-token") String sSessionId, @QueryParam("widget") String sWidget, @QueryParam("date") Long lDate) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("WidgetResource.getWidgetByTime: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		String sOrganizationId = oUser.getOrganizationId();
    		
    		if (Utils.isNullOrEmpty(sOrganizationId)) {
				RiseLog.warnLog("sOrganizationId.getWidgetByTime: Area id null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}    		
			
    		// Check if we have this subscription
    		OrganizationRepository oOrgRepository = new OrganizationRepository();
    		Organization oOrganization = (Organization) oOrgRepository.get(sOrganizationId);
			
    		if (oOrganization == null) {
				RiseLog.warnLog("WidgetResource.getWidgetByTime: Organization with this id " + sOrganizationId + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		if (!PermissionsUtils.canUserAccessOrganization(oOrganization, oUser)) {
				RiseLog.warnLog("WidgetResource.getWidgetByTime: user cannot access org");
				return Response.status(Status.UNAUTHORIZED).build();     			
    		}
    		
			if (lDate == null)
				lDate = 0L;

			double dDate = (double) lDate;

			if (dDate <= 0.0)
				dDate = DateUtils.getNowAsDouble()/1000;
    		
    		
    		WidgetInfoRepository oWidgetInfoRepository = new WidgetInfoRepository();
    		WidgetInfo oWidgetInfo = oWidgetInfoRepository.getByWidgetOrganizationIdTime(sWidget, sOrganizationId, dDate);
    		
    		if (oWidgetInfo != null) {
    			WidgetInfoViewModel oWidgetInfoViewModel = (WidgetInfoViewModel) RiseViewModel.getFromEntity(WidgetInfoViewModel.class.getName(), oWidgetInfo);
    			
    			AreaRepository oAreaRepository = new AreaRepository();
    			Area oArea = (Area) oAreaRepository.get(oWidgetInfo.getAreaId());
    			if (oArea != null) {
    				oWidgetInfoViewModel.areaName = oArea.getName();
    			}
    			else {
    				oWidgetInfoViewModel.areaName = oWidgetInfo.getAreaId();
    			}
    			
    			return Response.ok(oWidgetInfoViewModel).build();	
    		}
    		else {
    			return Response.ok().build();
    		}
    		    		
    		
		}
		catch (Exception oEx) {
			RiseLog.errorLog("WidgetResource.getWidgetByTime: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 		
	}

}
