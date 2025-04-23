package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.OTP;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

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
	
	
	public List<OTP> getByUserActionValidated(String sUserId, String sOperation, boolean bValidated) {
		List<OTP> aoReturnList = new ArrayList<OTP>();
		
		try {
			FindIterable<Document> aoDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("operation", sOperation), Filters.eq("validated", bValidated)));
            
            fillList(aoReturnList, aoDocuments, OTP.class);

            return aoReturnList;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("OTPRepository.getByUserActionValidated exception: " + oEx.toString());
		}
		
		return aoReturnList;
		
	}
	
}
