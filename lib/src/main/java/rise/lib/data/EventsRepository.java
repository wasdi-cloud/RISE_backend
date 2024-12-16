package rise.lib.data;

import rise.lib.business.Event;

public class EventsRepository extends MongoRepository {
	public EventsRepository() {
		m_sThisCollection = "events";
		m_oEntityClass = Event.class;
	}

}
