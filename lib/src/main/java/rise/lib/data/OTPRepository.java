package rise.lib.data;

import com.mongodb.BasicDBObject;

import rise.lib.business.OTP;
import rise.lib.utils.Utils;

public class OTPRepository extends MongoRepository {

	public OTPRepository() {
		m_sThisCollection = "otps";
		m_oEntityClass = OTP.class;
	}
	
	public OTP getOTP(String sId) {
		
		if (Utils.isNullOrEmpty(sId)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("id", sId);

        return (OTP) get(oCriteria);	
	}
	
	public boolean updateOPT(OTP oOTP) {

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("id", oOTP.getId());
		
		return update(oCriteria, oOTP);
	}
	
}
