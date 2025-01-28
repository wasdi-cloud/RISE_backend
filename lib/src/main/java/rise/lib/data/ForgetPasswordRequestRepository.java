package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.ForgetPasswordRequest;
import rise.lib.utils.Utils;

public class ForgetPasswordRequestRepository extends MongoRepository {
	public ForgetPasswordRequestRepository() {
		m_sThisCollection = "forget_password_request";
		m_oEntityClass = ForgetPasswordRequest.class;
	}

	/**
	 * Get a Forget password request from userId
	 * 
	 * @param sUserId
	 * @return
	 */
	public ForgetPasswordRequest getForgetPasswordRequestByUserId(String sUerId) {

		if (Utils.isNullOrEmpty(sUerId))
			return null;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", sUerId);

		return (ForgetPasswordRequest) get(oCriteria);
	}

}
