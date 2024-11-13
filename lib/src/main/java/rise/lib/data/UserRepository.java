package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.User;
import rise.lib.business.UserRole;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

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
	public boolean updateUserByEmail(User oUser) {

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("email", oUser.getEmail());
		
		return update(oCriteria, oUser);
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
	
	public User getUserByEmail(String sUserEmail) {
		
		if (Utils.isNullOrEmpty(sUserEmail)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("email", sUserEmail);

        return (User) get(oCriteria);		
	}	
	
	public List<User> getAdminsOfOrganization(String sOrganizationId) {
		
    	List<User> aoReturnList = new ArrayList<User>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("organizationId", sOrganizationId), Filters.or(Filters.eq("role", UserRole.ADMIN.name()), Filters.eq("role", UserRole.RISE_ADMIN.name()))));        	
        	
        	fillList(aoReturnList, oWSDocument, User.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	RiseLog.errorLog("UserRepository.getAdminsOfOrganization: error", oEx);
        }

        return aoReturnList;	
	}	
	
	public List<User> getHQOperatorsOfOrganization(String sOrganizationId) {
		
    	List<User> aoReturnList = new ArrayList<User>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(
        																				Filters.eq("organizationId", sOrganizationId), 
        																				Filters.eq("role", UserRole.HQ.name()
        																						)
        																				));        	
        	
        	fillList(aoReturnList, oWSDocument, User.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	RiseLog.errorLog("UserRepository.getAdminsOfOrganization: error", oEx);
        }

        return aoReturnList;	
	}	
	public int deleteByUserId(String sUserId) {
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", sUserId);
		return delete(oCriteria);
	}
	
}
