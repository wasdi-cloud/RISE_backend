package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import rise.lib.business.Event;
import rise.lib.utils.log.RiseLog;

public class EventsRepository extends MongoRepository {
	public EventsRepository() {
		m_sThisCollection = "events";
		m_oEntityClass = Event.class;
	}

	public List<Event> getByAreaId(String sAreaId) {
		List<Event> aoReturnList = new ArrayList<Event>();

		try {

			FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("areaId", sAreaId));

			fillList(aoReturnList, oWSDocument, Event.class);

			return aoReturnList;

		} catch (Exception oEx) {
			RiseLog.errorLog("EventsRepository.getByAreaId: error", oEx);
		}

		return aoReturnList;
	}

	public List<Event> getOngoingByAreaId(String sAreaId) {
		List<Event> aoReturnList = new ArrayList<Event>();

		try {

			FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("areaId", sAreaId), Filters.eq("inGoing", true)));

			fillList(aoReturnList, oWSDocument, Event.class);

			return aoReturnList;

		} catch (Exception oEx) {
			RiseLog.errorLog("EventsRepository.getOngoingByAreaId: error", oEx);
		}

		return aoReturnList;
	}
	
}
