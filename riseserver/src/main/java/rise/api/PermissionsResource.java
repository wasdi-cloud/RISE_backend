package rise.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.ResourceTypes;
import rise.lib.business.User;
import rise.lib.business.UserAccessRights;
import rise.lib.business.UserResourcePermission;
import rise.lib.data.AreaRepository;
import rise.lib.data.UserRepository;
import rise.lib.data.UserResourcePermissionRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;

@Path("permission")
public class PermissionsResource {

	@GET
	@Path("/resourcePermissions")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response findResourcePermissions(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sUserId) {

		RiseLog.debugLog("PermissionsResource.findResourcePermissions(" + " ResourceType: " + sResourceType + ", ResourceId: " + sResourceId + ", User: " + sUserId + " )");

		// Validate Session
		User oRequesterUser = Rise.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			RiseLog.debugLog("PermissionsResource.findResourcePermissions: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		return Response.ok().build();
	}

	@POST
	@Path("/add")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response addResourcePermission(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sDestinationUserId, @QueryParam("rights") String sRights) {

		RiseLog.debugLog("PermissionsResource.addResourcePermission(" + " ResourceType: " + sResourceType + ", ResourceId: " + sResourceId + ", User: " + sDestinationUserId + " )");
		
		// Validate Session
		User oRequesterUser = Rise.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			RiseLog.debugLog("PermissionsResource.addResourcePermission: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}		
		
		UserRepository oUserRepository = new UserRepository();
		User oTargetUser = oUserRepository.getUser(sDestinationUserId);
		
		if (oTargetUser == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}

		if (Utils.isNullOrEmpty(sResourceType)) {
			RiseLog.debugLog("PermissionsResource.addResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).build();
		}

		if (sResourceType.equalsIgnoreCase(ResourceTypes.AREA.getResourceType())) {
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sResourceId);
			
			if (oArea == null) {
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!oArea.getOrganizationId().equals(oRequesterUser.getOrganizationId())) {
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserWriteArea(oArea, oRequesterUser)) {
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// If the user is in the same org, can be invited as field user not shared area!!
			if (oArea.getOrganizationId().equals(oTargetUser.getOrganizationId())) {
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			UserResourcePermission oTestExisting = oUserResourcePermissionRepository.getPermissionByTypeUserIdResourceId(sResourceType, sDestinationUserId, sResourceId);
			if (oTestExisting != null) {
				return Response.ok().build();
			}
			
			UserResourcePermission oUserResourcePermission = new UserResourcePermission();
			oUserResourcePermission.setId(Utils.getRandomName());
			oUserResourcePermission.setCreatedBy(oRequesterUser.getUserId());
			oUserResourcePermission.setCreatedDate(DateUtils.nowInMillis());
			oUserResourcePermission.setOwnerId(oRequesterUser.getUserId());
			oUserResourcePermission.setPermissions(sRights);
			oUserResourcePermission.setResourceId(sResourceId);
			oUserResourcePermission.setResourceType(sResourceType);
			oUserResourcePermission.setUserId(oTargetUser.getUserId());
			
			
			oUserResourcePermissionRepository.add(oUserResourcePermission);
			
			return Response.ok().build();
			
		}
		else {
			RiseLog.debugLog("PermissionsResource.addResourcePermission: invalid resource type");

			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@DELETE
	@Path("/delete")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response removeResourcePermission(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sUserId) {

		RiseLog.debugLog("PermissionsResource.removeResourcePermission(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sUserId + " )");
		
		// Validate Session
		User oRequesterUser = Rise.getUserFromSession(sSessionId);
		
		if (oRequesterUser == null) {
			RiseLog.debugLog("PermissionsResource.addResourcePermission: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		UserRepository oUserRepository = new UserRepository();
		User oTargetUser = oUserRepository.getUser(sUserId);
		
		if (oTargetUser == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}		
		
		if (Utils.isNullOrEmpty(sResourceType)) {
			RiseLog.debugLog("PermissionsResource.addResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).build();
		}

		if (sResourceType.equalsIgnoreCase(ResourceTypes.AREA.getResourceType())) {
			
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			UserResourcePermission oUserResourcePermission = oUserResourcePermissionRepository.getPermissionByTypeUserIdResourceId(sResourceType, sUserId, sResourceId);
			
			if (oUserResourcePermission == null) {
				return Response.status(Status.BAD_REQUEST).build();	
			}
			
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sResourceId);
			
			if (oArea == null) {
				RiseLog.errorLog("There is a user resource permission id = " + oUserResourcePermission.getId() + " that links a not existing area " + sResourceId);
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			boolean bDelete = false;
			
			if (oUserResourcePermission.getOwnerId().equals(oRequesterUser.getUserId())) {
				bDelete = true;
			}
			else if (PermissionsUtils.canUserWriteArea(oArea, oTargetUser)) {
				bDelete = true;
			}
			
			if (bDelete) {
				oUserResourcePermissionRepository.delete(oUserResourcePermission.getId());
				return Response.ok().build();
			}
			else {
				return Response.status(Status.UNAUTHORIZED).build();
			}
		}  
		else {
			RiseLog.debugLog("PermissionsResource.removeResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	/**
	 * Get the list of available Resource Types
	 * 
	 * @param sSessionId Session Id
	 * @return Array of strings with the names of the resource types
	 */
	@GET
	@Path("types")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getResourceTypes(@HeaderParam("x-session-token") String sSessionId) {
		RiseLog.debugLog("PermissionsResource.getResourceTypes");
		try {
			ArrayList<String> asResourceTypes = new ArrayList<>();
			
			List<ResourceTypes> aoOriginalList = Arrays.asList(ResourceTypes.values());
			
			for (ResourceTypes oResType : aoOriginalList) {
				asResourceTypes.add(oResType.name());
			}
			
			return Response.ok(asResourceTypes).build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getSubscriptionTypes: error ", oEx);
			return Response.serverError().build();			
		}		
	}
		
}
