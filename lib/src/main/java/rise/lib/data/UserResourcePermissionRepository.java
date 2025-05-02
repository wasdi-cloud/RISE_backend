package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.UserResourcePermission;
import rise.lib.utils.log.RiseLog;

public class UserResourcePermissionRepository extends MongoRepository {
	
	public UserResourcePermissionRepository() {
		m_sThisCollection = "userresourcepermissions";
		m_oEntityClass = UserResourcePermission.class;		
	}
	
	
	/**
	 * Get the list of permissions by resource type and owner
	 * @param sType
	 * @param sUserId
	 * @return
	 */
	public List<UserResourcePermission> getPermissionsByTypeAndOwnerId(String sType, String sUserId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("ownerId", sUserId)));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndOwnerId : exception ", oEx);
		}

		return aoReturnList;
	}
	
	/**
	 * Get the list of permissions by Resource Type and Target User
	 * @param sType
	 * @param sUserId
	 * @return
	 */
	public List<UserResourcePermission> getPermissionsByTypeAndUserId(String sType, String sUserId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId)));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndUserId : exception ", oEx);
		}

		return aoReturnList;
	}	
	
	/**
	 * Get the list of permissions associated to a resource
	 * @param sType
	 * @param sResourceId
	 * @return
	 */
	public List<UserResourcePermission> getPermissionsByTypeAndResourceId(String sType, String sResourceId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("resourceId", sResourceId)));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndResourceId : exception ", oEx);
		}

		return aoReturnList;
	}
	
	/**
	 * Get the Permissions for a specific triple: Type, Target User, Resource Id
	 * @param sType
	 * @param sUserId
	 * @param sResourceId
	 * @return
	 */
	public UserResourcePermission getPermissionByTypeUserIdResourceId(String sType, String sUserId, String sResourceId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId)))
					.first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				UserResourcePermission oResourceSharing;

				oResourceSharing = s_oMapper.readValue(sJSON, UserResourcePermission.class);
				
				return oResourceSharing;
			}
		} catch (Exception oE) {
			RiseLog.errorLog("UserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId( " + sUserId+ ", " + sResourceId + "): error: ", oE);
		}

		return null;
	}	
}
