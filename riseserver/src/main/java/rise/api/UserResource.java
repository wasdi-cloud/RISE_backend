
package rise.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.User;
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

}