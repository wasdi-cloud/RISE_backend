
package rise.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.User;
import rise.lib.data.UserRepository;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.UserViewModel;

@Path("usr")
public class UserResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@HeaderParam("x-session-token") String sSessionId) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("UserResource.getUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			UserViewModel oUserViewModel = (UserViewModel) RiseViewModel.getFromEntity(UserViewModel.class.getName(),
					oUser);

			return Response.ok(oUserViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.getUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	public Response updateUser(@HeaderParam("x-session-token") String sSessionId, UserViewModel oUserViewModel) {
		try {

			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("UserResource.updateUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (oUserViewModel == null) {
				RiseLog.warnLog("UserResource.updateUser: user VM null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (oUserViewModel.name == null) {
				RiseLog.warnLog("UserResource.updateUser: user name null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (oUserViewModel.surname == null) {
				RiseLog.warnLog("UserResource.updateUser: user surname null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (oUserViewModel.mobile == null) {
				RiseLog.warnLog("UserResource.updateUser: user mobile null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// update name , surname and mobile in user entity
			oUser.setName(oUserViewModel.name);
			oUser.setSurname(oUserViewModel.surname);
			oUser.setMobile(oUserViewModel.mobile);

			UserRepository oUserRepository = new UserRepository();
			oUserRepository.updateUser(oUser);
			
			
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.updateUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}