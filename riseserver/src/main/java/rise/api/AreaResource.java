package rise.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.User;
import rise.lib.data.AreaRepository;
import rise.lib.data.UserRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.AreaListViewModel;
import rise.lib.viewmodels.AreaViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.UserOfAreaViewModel;

@Path("area")
public class AreaResource {
	
	@GET
	@Path("list")
	public Response getList(@HeaderParam("x-session-token") String sSessionId) {
		
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("AreaResource.getList: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		// We need an admin here!
    		if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.getList: cannot handle area");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}
    		
    		AreaRepository oAreaRepository = new AreaRepository();
    		List<Area> aoAreas = oAreaRepository.getByOrganization(oUser.getOrganizationId());
    		
    		// Get the areas of this org    		
    		// Create VM list
			ArrayList<AreaListViewModel> aoAreasVM = new ArrayList<>();
			
			// Convert the entities
			for (Area oArea : aoAreas) {
				
				AreaListViewModel oListItem = (AreaListViewModel) RiseViewModel.getFromEntity(AreaListViewModel.class.getName(), oArea);
				aoAreasVM.add(oListItem);
			}
			
			// return the list to the client
			return Response.ok(aoAreasVM).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getList: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
	
	/**
	 * Get an Area by id
	 * @param sSessionId
	 * @param sId
	 * @return
	 */
	@GET
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId) {
		
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("AreaResource.getById: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		// We need an admin here!
    		if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.getById: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}
    		
    		if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("AreaResource.getById: id null");
				return Response.status(Status.BAD_REQUEST).build();      			    			
    		}
    		
    		// Get the this area
    		AreaRepository oAreaRepository = new AreaRepository();
    		Area oArea = (Area) oAreaRepository.get(sId);
    		
    		if (oArea == null) {
				RiseLog.warnLog("AreaResource.getById: area " + sId + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		AreaViewModel oAreaViewModel = (AreaViewModel) RiseViewModel.getFromEntity(AreaViewModel.class.getName(), oArea);
			
			// return the list to the client
			return Response.ok(oAreaViewModel).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
	
	@PUT
	public Response update(@HeaderParam("x-session-token") String sSessionId, AreaViewModel oAreaViewModel) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("AreaResource.update: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		// We need an admin here!
    		if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.update: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}
    		
    		if (oAreaViewModel == null) {
				RiseLog.warnLog("AreaResource.update: Area null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		if (Utils.isNullOrEmpty(oAreaViewModel.id)) {
				RiseLog.warnLog("AreaResource.update: Area id null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}    		
			
    		// Check if we have this subscription
    		AreaRepository oAreaRepository = new AreaRepository();
    		Area oFromDbArea = (Area) oAreaRepository.get(oAreaViewModel.id);
			
    		if (oFromDbArea == null) {
				RiseLog.warnLog("AreaResource.update: Area with this id " + oAreaViewModel.id + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// Create the updated entity
    		Area oArea = (Area) RiseViewModel.copyToEntity(Area.class.getName(), oAreaViewModel);
    		
    		// Copy the admitted updates in the entity
    		oFromDbArea.setDescription(oArea.getDescription());
    		oFromDbArea.setName(oArea.getName());
    		oFromDbArea.setPlugins(oArea.getPlugins());
    		
    		// Update it
    		if (!oAreaRepository.update(oFromDbArea)) {
				RiseLog.warnLog("AreaResource.update: There was an error updating the area");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    		}
    		else {
    			return Response.ok().build();
    		}
		}
		catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.update: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}		
	}
	
	
	@GET
	@Path("users")
	public Response getUsers(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId) {
		
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("AreaResource.getUsers: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.getUsers: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}
    		
    		if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("AreaResource.getUsers: id null");
				return Response.status(Status.BAD_REQUEST).build();      			    			
    		}
    		
    		// Get the this area
    		AreaRepository oAreaRepository = new AreaRepository();
    		Area oArea = (Area) oAreaRepository.get(sId);
    		
    		if (oArea == null) {
				RiseLog.warnLog("AreaResource.getUsers: area " + sId + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		UserRepository oUserRepository = new UserRepository();
    		
    		ArrayList<UserOfAreaViewModel> aoUsersVM = new ArrayList<>();
    		
    		for (String sUserId : oArea.getFieldOperators()) {
				
    			if (Utils.isNullOrEmpty(sUserId)) {
    				RiseLog.warnLog("AreaResource.getUsers: user id null, jump area Id: " + sId);
    				continue;
    			}
    			
    			User oFieldUser = oUserRepository.getUser(sUserId);
    			
    			if (oFieldUser == null) {
    				RiseLog.warnLog("AreaResource.getUsers: user id  " + sUserId + " not corresponding to a valid user, jump");
    				continue;
    			}
    			
    			UserOfAreaViewModel oUserAreaVM = (UserOfAreaViewModel) RiseViewModel.getFromEntity(UserOfAreaViewModel.class.getName(), oFieldUser);
    			oUserAreaVM.areaId = sId;
    			aoUsersVM.add(oUserAreaVM);
			}
			
			// return the list to the client
			return Response.ok(aoUsersVM).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
	
	@POST
	@Path("users")
	public Response addUser(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId, UserOfAreaViewModel oUserToAdd) {
		
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("AreaResource.addUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		// We need an admin here!
    		if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.addUser: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}
    		
    		if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("AreaResource.addUser: id null");
				return Response.status(Status.BAD_REQUEST).build();      			    			
    		}
    		
    		if (oUserToAdd==null) {
				RiseLog.warnLog("AreaResource.addUser: user view model null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		if (Utils.isNullOrEmpty(oUserToAdd.userId)) {
				RiseLog.warnLog("AreaResource.addUser: user view model has user id null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// Get the this area
    		AreaRepository oAreaRepository = new AreaRepository();
    		Area oArea = (Area) oAreaRepository.get(sId);
    		
    		if (oArea == null) {
				RiseLog.warnLog("AreaResource.addUser: area " + sId + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		UserRepository oUserRepository = new UserRepository();
    		User oFieldUser = oUserRepository.getUser(oUserToAdd.userId);
    		
    		if (oFieldUser == null) {
				RiseLog.warnLog("AreaResource.addUser: user " + oUserToAdd.userId + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		if (oArea.getFieldOperators().contains(oFieldUser.getUserId())) {
				RiseLog.warnLog("AreaResource.addUser: user " + oUserToAdd.userId + " already in the area");
				return Response.ok().build();    			
    		}
    		
    		oArea.getFieldOperators().add(oUserToAdd.userId);
    		oAreaRepository.update(oArea);
    
			// return the list to the client
			return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}		

}
