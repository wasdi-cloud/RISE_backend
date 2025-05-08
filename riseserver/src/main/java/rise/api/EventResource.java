package rise.api;

import java.util.ArrayList;
import java.util.List;
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
import rise.lib.business.Event;
import rise.lib.business.AttachmentsCollections;
import rise.lib.business.Subscription;
import rise.lib.business.User;
import rise.lib.data.AreaRepository;
import rise.lib.data.EventsRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.AttachmentListViewModel;
import rise.lib.viewmodels.ErrorViewModel;
import rise.lib.viewmodels.EventViewModel;
import rise.lib.viewmodels.RiseViewModel;

@Path("event")
public class EventResource {

	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getList(@HeaderParam("x-session-token") String sSessionId, @QueryParam("areaId") String sAreaId) {

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

			EventsRepository oEventsRepository = new EventsRepository();
			List<Event> aoEvents = oEventsRepository.getByAreaId(sAreaId);
			List<EventViewModel> aoEventVM = new ArrayList<>();

			for (Event oEvent : aoEvents) {
				EventViewModel oEventViewModel = (EventViewModel) RiseViewModel
						.getFromEntity(EventViewModel.class.getName(), oEvent);
				aoEventVM.add(oEventViewModel);
			}
			// return the list to the client
			return Response.ok(aoEventVM).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("EventResource.getList: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("add")
	@Produces(MediaType.APPLICATION_JSON)
	public Response addEvent(@HeaderParam("x-session-token") String sSessionId, EventViewModel oEventViewModel,
			@QueryParam("areaId") String sAreaId) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("EventResource.addEvent: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (oEventViewModel == null) {
				RiseLog.warnLog("EventResource.addEvent: Event null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (Utils.isNullOrEmpty(sAreaId)) {
				RiseLog.warnLog("EventResource.addEvent: area id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			Subscription oValidSubscription = PermissionsUtils.getValidSubscription(oUser);

			if (oValidSubscription == null) {
				RiseLog.warnLog(
						"EventResource.addEvent: user " + oUser.getUserId() + " does not have a valid subscription");
				ErrorViewModel oError = new ErrorViewModel(StringCodes.ERROR_API_NO_VALID_SUBSCRIPTION.name());
				return Response.status(Status.FORBIDDEN).entity(oError).build();
			}

			// Check if we have this event
			EventsRepository oEventsRepository = new EventsRepository();

			// Create the entity
			Event oEvent = (Event) RiseViewModel.copyToEntity(Event.class.getName(), oEventViewModel);
			// todo will do images and docs and other attributes later
			oEvent.setAreaId(sAreaId);
			oEvent.setId(Utils.getRandomName());
			// Create it
			oEventsRepository.add(oEvent);

			EventViewModel oNewEventViewModel = (EventViewModel) RiseViewModel.getFromEntity(EventViewModel.class.getName(), oEvent);
			return Response.ok(oNewEventViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("EventResource.addEvent: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PUT
	@Path("update")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateEvent(@HeaderParam("x-session-token") String sSessionId, EventViewModel oEventViewModel) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("EventResource.updateEvent: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (oEventViewModel == null) {
				RiseLog.warnLog("EventResource.updateEvent: Event null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (Utils.isNullOrEmpty(oEventViewModel.id)) {
				RiseLog.warnLog("EventResource.updateEvent: Event id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Check if we have this subscription
			EventsRepository oEventsRepository = new EventsRepository();
			Event oFromDbEvent = (Event) oEventsRepository.get(oEventViewModel.id);

			if (oFromDbEvent == null) {
				RiseLog.warnLog("EventResource.updateEvent: Event with this id " + oEventViewModel.id + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// map the values if they are not null

			if (!Utils.isNullOrEmpty(oEventViewModel.bbox)) {
				oFromDbEvent.setBbox(oEventViewModel.bbox);
			}
			if (!Utils.isNullOrEmpty(oEventViewModel.description)) {
				oFromDbEvent.setDescription(oEventViewModel.description);
			}
			if (!Utils.isNullOrEmpty(oEventViewModel.name)) {
				oFromDbEvent.setName(oEventViewModel.name);
			}
			if (oEventViewModel.type!=null) {
				oFromDbEvent.setType(oEventViewModel.type);
			}
			if (!Utils.isNullOrEmpty(oEventViewModel.endDate)) {
				oFromDbEvent.setEndDate(oEventViewModel.endDate);
			}
			if (!Utils.isNullOrEmpty(oEventViewModel.peakDate)) {
				oFromDbEvent.setPeakDate(oEventViewModel.peakDate);
			}
			if (!Utils.isNullOrEmpty(oEventViewModel.startDate)) {
				oFromDbEvent.setStartDate(oEventViewModel.startDate);
			}
			oFromDbEvent.setInGoing(oEventViewModel.inGoing);
			oFromDbEvent.setPublicEvent(oEventViewModel.publicEvent);
			
			// Update it
			if (!oEventsRepository.update(oFromDbEvent, oFromDbEvent.getId())) {
				RiseLog.warnLog("EventResource.updateEvent: There was an error updating the event");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} else {
				return Response.ok().build();
			}

		} catch (Exception oEx) {
			RiseLog.errorLog("EventResource.updateEvent: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DELETE
	@Path("delete")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteEvent(@HeaderParam("x-session-token") String sSessionId, @QueryParam("eventId") String sEventId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("EventResource.deleteEvent: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("EventResource.deleteEvent: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sEventId)) {
				RiseLog.warnLog("EventResource.deleteEvent: id null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			EventsRepository oEventsRepository = new EventsRepository();
			Event oEvent = (Event) oEventsRepository.get(sEventId);
			if (oEvent == null) {
				RiseLog.warnLog("EventResource.deleteEvent: event  does not exist");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			cleanAttachmentsForEvent(sSessionId, AttachmentsCollections.EVENTS_DOCS, sEventId);
			cleanAttachmentsForEvent(sSessionId, AttachmentsCollections.EVENTS_IMAGES, sEventId);
			
			oEventsRepository.delete(sEventId);
			
			return Response.ok().build();
		} 
		catch (Exception oEx) {
			RiseLog.errorLog("EventResource.updateEvent: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Clean all the attachments for an event
	 * @param sSessionId
	 * @param oCollection
	 * @param sEventId
	 * @return
	 */
	private boolean cleanAttachmentsForEvent(String sSessionId, AttachmentsCollections oCollection, String sEventId) {
		try {
			AttachmentResource oAttachmentResource = new AttachmentResource();
			
			Response oResponse = oAttachmentResource.list(sSessionId, oCollection.getFolder(), sEventId);
			if (oResponse.getEntity() != null) {
				AttachmentListViewModel oAttachmentListViewModel = (AttachmentListViewModel) oResponse.getEntity();
				
				for (String sAttachment : oAttachmentListViewModel.files) {
					oAttachmentResource.delete(sSessionId, oCollection.getFolder(), sEventId, sAttachment);
				}
			}			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("EventResource.cleanAttachmentsForEvent: " + oEx);
			return false;
		}
		
		return true;
		
	}

}
