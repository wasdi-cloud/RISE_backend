package rise.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.lib.config.RiseConfig;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import wasdi.jwasdilib.WasdiLib;

@Path("wasdi")
public class WasdiResource {

	@GET
	@Path("launch-wasdi-app")
	public Response launchWasdiApp() {
		try {
			// const for test
			String sWorkspaceId = "b4b4868d-00db-4c97-8f6b-cf394eb1c21f";
			WasdiLib oWasdiLib = new WasdiLib();
			oWasdiLib.setUser(RiseConfig.Current.wasdiConfig.wasdiUser);
			oWasdiLib.setPassword(RiseConfig.Current.wasdiConfig.wasdiPassword);
			
			if (oWasdiLib.init()) {
				oWasdiLib.openWorkspaceById(sWorkspaceId);
				String sProcessId = oWasdiLib.executeProcessor("viirs_flood", "{}");
				

				List<String> oList=oWasdiLib.getProductsByActiveWorkspace();
				String sLink = oWasdiLib.getPath(oList.get(0));
				
				if (!Utils.isNullOrEmpty(sLink)) {
					return Response.ok(sLink).header("Content-Disposition", "attachment; filename=" + oList.get(0)).build();
				} else {
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				}

			} else {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

		} catch (Exception oEx) {
			RiseLog.errorLog("LayerResource.downloadLayer: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
}
