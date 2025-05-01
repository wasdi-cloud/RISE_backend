package rise.api;

import java.awt.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
import rise.lib.business.Subscription;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.AreaRepository;
import rise.lib.data.LayerRepository;
import rise.lib.data.UserRepository;
import rise.lib.data.WasdiTaskRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.AreaListViewModel;
import rise.lib.viewmodels.AreaViewModel;
import rise.lib.viewmodels.ErrorViewModel;
import rise.lib.viewmodels.OverlappingAreaViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.UserOfAreaViewModel;
import wasdi.jwasdilib.WasdiLib;

@Path("area")
public class AreaResource {

	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
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

				AreaListViewModel oListItem = (AreaListViewModel) RiseViewModel
						.getFromEntity(AreaListViewModel.class.getName(), oArea);
				aoAreasVM.add(oListItem);
			}

			// return the list to the client
			return Response.ok(aoAreasVM).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getList: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("list-by-user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.getListByUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			ArrayList<AreaListViewModel> aoAreasVM = new ArrayList<>();
			AreaRepository oAreaRepository = new AreaRepository();
			// Get the areas of this org
			List<Area> aoAreas = oAreaRepository.getByOrganization(oUser.getOrganizationId());
			if (PermissionsUtils.hasHQRights(oUser)) {
				// Convert the entities
				for (Area oArea : aoAreas) {

					AreaListViewModel oListItem = (AreaListViewModel) RiseViewModel
							.getFromEntity(AreaListViewModel.class.getName(), oArea);
					aoAreasVM.add(oListItem);
				}

			} else if (PermissionsUtils.hasFieldRights(oUser)) {
				for (Area oArea : aoAreas) {
					if (oArea.getFieldOperators() != null && oArea.getFieldOperators().contains(oUser.getUserId())) {
						AreaListViewModel oListItem = (AreaListViewModel) RiseViewModel
								.getFromEntity(AreaListViewModel.class.getName(), oArea);
						aoAreasVM.add(oListItem);
					}
				}
			} else {
				RiseLog.warnLog("AreaResource.getListByUser: cannot handle area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// return the list to the client
			return Response.ok(aoAreasVM).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getListByUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get an Area by id
	 * 
	 * @param sSessionId
	 * @param sId
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.getById: invalid Session");
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

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("AreaResource.getById: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			AreaViewModel oAreaViewModel = (AreaViewModel) RiseViewModel.getFromEntity(AreaViewModel.class.getName(),
					oArea);

			// return the list to the client
			return Response.ok(oAreaViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@HeaderParam("x-session-token") String sSessionId, AreaViewModel oAreaViewModel) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.update: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

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

			if (!PermissionsUtils.canUserAccessArea(oFromDbArea, oUser)) {
				RiseLog.warnLog("AreaResource.update: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Create the updated entity
			Area oArea = (Area) RiseViewModel.copyToEntity(Area.class.getName(), oAreaViewModel);

			// Copy the admitted updates in the entity
			oFromDbArea.setDescription(oArea.getDescription());
			oFromDbArea.setName(oArea.getName());
			oFromDbArea.setPlugins(oArea.getPlugins());

			// Update it
			if (!oAreaRepository.update(oFromDbArea, oFromDbArea.getId())) {
				RiseLog.warnLog("AreaResource.update: There was an error updating the area");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} else {
				return Response.ok().build();
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.update: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response add(@HeaderParam("x-session-token") String sSessionId, AreaViewModel oAreaViewModel) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.add: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.add: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (oAreaViewModel == null) {
				RiseLog.warnLog("AreaResource.add: Area null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			Subscription oValidSubscription = PermissionsUtils.getValidSubscription(oUser);

			if (oValidSubscription == null) {
				RiseLog.warnLog("AreaResource.add: user " + oUser.getUserId() + " does not have a valid subscription");
				ErrorViewModel oError = new ErrorViewModel(StringCodes.ERROR_API_NO_VALID_SUBSCRIPTION.name());
				return Response.status(Status.FORBIDDEN).entity(oError).build();
			}
			
			boolean bCanAddAreas = PermissionsUtils.canUserAddArea(oUser);
			
			if (!bCanAddAreas) {
				RiseLog.warnLog("SubscriptionResource.update: the org does not have more free areas");
				ErrorViewModel oError = new ErrorViewModel(StringCodes.ERROR_API_NO_VALID_SUBSCRIPTION.name());
				return Response.status(Status.UNAUTHORIZED).entity(oError).build();				
			}
			

			// Check if we have this subscription
			AreaRepository oAreaRepository = new AreaRepository();

			// Create the updated entity
			Area oArea = (Area) RiseViewModel.copyToEntity(Area.class.getName(), oAreaViewModel);

			Double dNow = DateUtils.getNowAsDouble();
			oArea.setCreationDate(dNow);
			oArea.setId(Utils.getRandomName());
			oArea.setSubscriptionId(oValidSubscription.getId());
			oArea.setOrganizationId(oUser.getOrganizationId());
			oArea.setArchiveStartDate(-1.0);
			oArea.setArchiveEndDate(-1.0);
			oArea.setActive(true);

			// Create it
			oAreaRepository.add(oArea);

			AreaViewModel oNewAreaViewModel = (AreaViewModel) RiseViewModel.getFromEntity(AreaViewModel.class.getName(),
					oArea);

			return Response.ok(oNewAreaViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.add: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
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

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("AreaResource.getUsers: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			UserRepository oUserRepository = new UserRepository();
			ArrayList<UserOfAreaViewModel> aoUsersVM = new ArrayList<>();
			List<String> asFieldOperatorsIds = oArea.getFieldOperators();

			// check if there are any field operators
			if (asFieldOperatorsIds != null && !asFieldOperatorsIds.isEmpty()) {
				for (String sFieldOperatorId : asFieldOperatorsIds) {
					if (Utils.isNullOrEmpty(sFieldOperatorId)) {
						RiseLog.warnLog("AreaResource.getUsers: user id  null");
						continue;

					}
					User oFieldOperatorOfArea = oUserRepository.getUser(sFieldOperatorId);
					if (oFieldOperatorOfArea == null) {
						RiseLog.warnLog("AreaResource.getUsers: user " + sFieldOperatorId + "is not found");
						continue;
					}
					UserOfAreaViewModel oUserAreaVM = (UserOfAreaViewModel) RiseViewModel
							.getFromEntity(UserOfAreaViewModel.class.getName(), oFieldOperatorOfArea);
					oUserAreaVM.areaId = sId;
					aoUsersVM.add(oUserAreaVM);
				}
			}

			List<User> aoAdmins = oUserRepository.getAdminsOfOrganization(oUser.getOrganizationId());

			for (User oAdminUser : aoAdmins) {

				UserOfAreaViewModel oUserAreaVM = (UserOfAreaViewModel) RiseViewModel
						.getFromEntity(UserOfAreaViewModel.class.getName(), oAdminUser);
				oUserAreaVM.areaId = sId;
				aoUsersVM.add(oUserAreaVM);
			}

			List<User> aoHQOperators = oUserRepository.getHQOperatorsOfOrganization(oUser.getOrganizationId());
			if (!aoHQOperators.isEmpty()) {
				for (User oHQOperator : aoHQOperators) {

					UserOfAreaViewModel oUserAreaVM = (UserOfAreaViewModel) RiseViewModel
							.getFromEntity(UserOfAreaViewModel.class.getName(), oHQOperator);
					oUserAreaVM.areaId = sId;
					aoUsersVM.add(oUserAreaVM);
				}
			}

			// return the list to the client
			return Response.ok(aoUsersVM).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getUsers: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
	public Response addUser(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId,
			UserOfAreaViewModel oUserToAdd) {

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

			if (oUserToAdd == null) {
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

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("AreaResource.addUser: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			UserRepository oUserRepository = new UserRepository();
			User oFieldUser = oUserRepository.getUser(oUserToAdd.userId);

			if (oFieldUser == null) {
				RiseLog.warnLog("AreaResource.addUser: user " + oUserToAdd.userId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (oArea.getFieldOperators() != null) {
				if (oArea.getFieldOperators().contains(oFieldUser.getUserId())) {
					RiseLog.warnLog("AreaResource.addUser: user " + oUserToAdd.userId + " already in the area");
					return Response.ok().build();
				}
				oArea.getFieldOperators().add(oUserToAdd.userId);
				oAreaRepository.update(oArea, oArea.getId());
			} else {
				ArrayList<String> asListOfUsers = new ArrayList<String>();
				asListOfUsers.add(oUserToAdd.userId);
				oArea.setFieldOperators(asListOfUsers);
				oAreaRepository.update(oArea, oArea.getId());
			}
			// return the list to the client
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.addUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("field")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFieldOperators(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.getFieldOperators: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.getFieldOperators: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("AreaResource.getFieldOperators: id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Get the this area
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sId);

			if (oArea == null) {
				RiseLog.warnLog("AreaResource.getFieldOperators: area " + sId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("AreaResource.getFieldOperators: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			// return the list to the client
			return Response.ok(oArea.getFieldOperators()).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getFieldOperators: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DELETE
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteUser(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId,
			UserOfAreaViewModel oUserToDelete) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.deleteUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need some one that can handle the area here
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.deleteUser: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("AreaResource.deleteUser: id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (oUserToDelete == null) {
				RiseLog.warnLog("AreaResource.deleteUser: user view model null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (Utils.isNullOrEmpty(oUserToDelete.userId)) {
				RiseLog.warnLog("AreaResource.deleteUser: user view model has user id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Get the this area
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sId);

			if (oArea == null) {
				RiseLog.warnLog("AreaResource.deleteUser: area " + sId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!PermissionsUtils.canUserAccessArea(oArea, oUser)) {
				RiseLog.warnLog("AreaResource.deleteUser: user cannot access area");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			UserRepository oUserRepository = new UserRepository();
			User oFieldUser = oUserRepository.getUser(oUserToDelete.userId);

			if (oFieldUser == null) {
				RiseLog.warnLog("AreaResource.deleteUser: user " + oUserToDelete.userId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!oArea.getFieldOperators().contains(oFieldUser.getUserId())) {
				RiseLog.warnLog(
						"AreaResource.deleteUser: user " + oUserToDelete.userId + " is already not in the area");
				return Response.ok().build();
			}

			oArea.getFieldOperators().remove(oUserToDelete.userId);
			oAreaRepository.update(oArea, oArea.getId());

			// return the list to the client
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.deleteUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("check_area")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOverlappingAreas(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId,
			AreaViewModel oArea) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.getOverlappingAreas: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need some one that can handle the area here
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.getOverlappingAreas: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("AreaResource.getOverlappingAreas: id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (oArea == null) {
				RiseLog.warnLog("AreaResource.getOverlappingAreas: area view model null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			ArrayList<OverlappingAreaViewModel> aoOverlappingAreas = new ArrayList<>();
			AreaRepository oAreaRepository = new AreaRepository();
			List<Area> aoAreas = oAreaRepository.getByOrganization(sId);
			for (Area oAreaElement : aoAreas) {
				// check same name
				if (oArea.name.equals(oAreaElement.getName())) {
					ArrayList<String> asErrors = new ArrayList<>();
					asErrors.add(StringCodes.ERROR_API_AREA_NAME_ALREADY_EXISTS.name());
					ErrorViewModel oErrorViewModel = new ErrorViewModel(asErrors, Status.CONFLICT.getStatusCode());
					return Response.status(Status.CONFLICT).entity(oErrorViewModel).build();
				}
				// check overlapping
				if (checkIntersection(oArea.bbox, oAreaElement.getBbox())) {
					// create over lapping area vm
					OverlappingAreaViewModel oOverlappingAreaViewModel = new OverlappingAreaViewModel();
					oOverlappingAreaViewModel.bbox = oAreaElement.getBbox();
					oOverlappingAreaViewModel.id = oAreaElement.getId();
					oOverlappingAreaViewModel.name = oAreaElement.getName();
					aoOverlappingAreas.add(oOverlappingAreaViewModel);
				}
			}

			return Response.ok(aoOverlappingAreas).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.getOverlappingAreas: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private boolean checkIntersection(String sAreaToCreateBBox, String sAreaAlreadyExistsBBox) {
		Polygon oNewArea = parsePolygonWKT(sAreaToCreateBBox);
		Polygon oExistingArea = parsePolygonWKT(sAreaAlreadyExistsBBox);
		if (oNewArea.getBounds().intersects(oExistingArea.getBounds())) {
			return true;
		}
		return false;
	}

	public Polygon parsePolygonWKT(String sWkt) {

		String[] asCoords = sWkt.replaceAll("(?i)POLYGON\\s*\\(\\(", "").replace("))", "").split(",");
		int[] aiXPoints = new int[asCoords.length];
		int[] aiYPoints = new int[asCoords.length];

		for (int i = 0; i < asCoords.length; i++) {
			StringTokenizer tokenizer = new StringTokenizer(asCoords[i], " ");
			aiXPoints[i] = (int) Double.parseDouble(tokenizer.nextToken());
			aiYPoints[i] = (int) Double.parseDouble(tokenizer.nextToken());
		}

		return new Polygon(aiXPoints, aiYPoints, aiXPoints.length);
	}

	@DELETE
	@Path("delete-area")
	public Response deleteArea(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sAreaId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("AreaResource.deleteArea: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("AreaResource.deleteArea: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sAreaId)) {
				RiseLog.warnLog("AreaResource.deleteArea: area id null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);
			if (oArea == null) {
				RiseLog.warnLog("AreaResource.deleteArea: area does not exist");
				return Response.status(Status.BAD_REQUEST).build();
			}
			// delete the layers
			LayerRepository oLayerRepository = new LayerRepository();
			oLayerRepository.deleteByAreaId(sAreaId);
			// delete the wasdi tasks related to this area
			WasdiTaskRepository oWasdiTaskRepository = new WasdiTaskRepository();
			oWasdiTaskRepository.deleteByAreaId(sAreaId);
			// todo Clean all the workspaces related to that area
			WasdiLib oWasdiLib = new WasdiLib();
			oWasdiLib.setUser(RiseConfig.Current.wasdiConfig.wasdiUser);
			oWasdiLib.setPassword(RiseConfig.Current.wasdiConfig.wasdiPassword);
			
			if (oWasdiLib.init()) {
				// get the list of the workspaces
				List<Map<String, Object>> aoWorkspaces = oWasdiLib.getWorkspaces();
				// find the workspaces that start with the area id and delete those
				for (Map<String, Object> oWorkspace : aoWorkspaces) {
					Object oName = oWorkspace.get("workspaceName");
					Object oId = oWorkspace.get("workspaceId");

					if (oName instanceof String && oId instanceof String) {
						String sName = (String) oName;
						String sId = (String) oId;
						if (sName.startsWith(sAreaId)) {
							// Found a workspace that starts with the area ID
							// You can delete or process it here
							RiseLog.infoLog("AreaResource.deleteArea: Deleting Workspace " + sName + "with the Id : " + sId);

							// Example: delete the workspace
							oWasdiLib.deleteWorkspace(sId);
						}
					}
				}

			}
			oAreaRepository.delete(sAreaId);

			// RISE will stop the related subscription of the deleted Area of Operations

			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("AreaResource.deleteArea: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
