package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.User;
import rise.lib.utils.Utils;

public class UserRepository extends MongoRepository {
	
	public UserRepository() {
		m_sThisCollection = "users";
		m_oEntityClass = User.class;
	}
	
	
	/**
	 * Get a user from user id
	 * @param sUserId
	 * @return
	 */
	public User getUser(String sUserId) {
		
		if (Utils.isNullOrEmpty(sUserId)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", sUserId);

        return (User) get(oCriteria);		
	}
	
	/**
	 * Update a user
	 * @param oUser
	 * @return
	 */
	public boolean updateUser(User oUser) {

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", oUser.getUserId());
		
		return update(oCriteria, oUser);
	}
	
	public User getUserByEmain(String sUserEmail) {
		
		if (Utils.isNullOrEmpty(sUserEmail)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("email", sUserEmail);

        return (User) get(oCriteria);		
	}	
	
}
