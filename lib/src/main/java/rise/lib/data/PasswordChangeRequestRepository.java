package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.PasswordChangeRequest;
import rise.lib.utils.Utils;

public class PasswordChangeRequestRepository extends MongoRepository {

	public PasswordChangeRequestRepository() {
		m_sThisCollection = "password_change_request";
		m_oEntityClass = PasswordChangeRequest.class;
	}

	/**
	 * Get a password change request from otp id
	 * 
	 * @param sOTPId
	 * @return
	 */
	public PasswordChangeRequest getPasswordChangeRequestByOTPId(String sOTPId) {

		if (Utils.isNullOrEmpty(sOTPId))
			return null;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("otpId", sOTPId);

		return (PasswordChangeRequest) get(oCriteria);
	}

}