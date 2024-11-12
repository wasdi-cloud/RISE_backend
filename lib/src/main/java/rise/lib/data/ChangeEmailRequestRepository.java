package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.ChangeEmailRequest;

import rise.lib.utils.Utils;

public class ChangeEmailRequestRepository extends MongoRepository {

	public ChangeEmailRequestRepository() {
		m_sThisCollection = "change_email_request";
		m_oEntityClass = ChangeEmailRequest.class;
	}

	/**
	 * Get a user from user id
	 * 
	 * @param sUserId
	 * @return
	 */
	public ChangeEmailRequest getChangeEmailRequestByOldEmail(String sOldEmail) {

		if (Utils.isNullOrEmpty(sOldEmail))
			return null;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("oldEmail", sOldEmail);

		return (ChangeEmailRequest) get(oCriteria);
	}

}