package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.Session;
import rise.lib.utils.Utils;

public class SessionRepository extends MongoRepository {
	
	public SessionRepository() {
		m_sThisCollection = "sessions";
		m_oEntityClass = Session.class;
	}
	
	public Session getSession(String sToken) {
		
		if (Utils.isNullOrEmpty(sToken)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("token", sToken);

        return (Session) get(oCriteria);		
	}
		
}
