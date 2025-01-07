package rise.api;

import java.util.ArrayList;
import java.util.Iterator;
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
import rise.lib.business.Event;
import rise.lib.business.User;
import rise.lib.data.AreaRepository;
import rise.lib.data.EventsRepository;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.EventViewModel;
import rise.lib.viewmodels.RiseViewModel;

@Path("event")
public class EventResource {

	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getList(@HeaderParam("x-session-token") String sSessionId,@QueryParam("areaId") String sAreaId) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("EventResource.getList: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(sAreaId)) {
				RiseLog.warnLog("EventResource.getList: Area id is numm");
				return Response.status(Status.BAD_REQUEST).build();
			}
			AreaRepository oAreaRepository = new AreaRepository();
			Area oArea = (Area) oAreaRepository.get(sAreaId);
			if (oArea == null) {
				RiseLog.warnLog("EventResource.getList: area is not found");
				return Response.status(Status.NOT_FOUND).build();
			}
			
			EventsRepository oEventsRepository=new EventsRepository();
			List<Event> aoEvents=oEventsRepository.getByAreaId(sAreaId);
			List<EventViewModel> aoEventVM=new ArrayList<>();
			
			for (Event oEvent : aoEvents) {
				EventViewModel oEventViewModel=(EventViewModel)RiseViewModel.getFromEntity(EventViewModel.class.getName(), oEvent);
				aoEventVM.add(oEventViewModel);
			}
			// return the list to the client
			return Response.ok(aoEventVM).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("EventResource.getList: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}
