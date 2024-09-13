package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.User;
import rise.lib.utils.Utils;

public class UserRepository extends MongoRepository {
	
	public UserRepository() {
		m_sThisCollection = "users";
		m_oEntityClass = User.class;
	}
	
	
	public User getUser(String sUserId) {
		
		if (Utils.isNullOrEmpty(sUserId)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", sUserId);

        return (User) get(oCriteria);		
	}
	
}
