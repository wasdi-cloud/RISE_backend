package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;

import rise.lib.business.OTP;
import rise.lib.business.SubscriptionType;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

public class SubscriptionTypeRepository extends MongoRepository {
	public SubscriptionTypeRepository() {
		m_sThisCollection = "subscription_types";
		m_oEntityClass = SubscriptionType.class;
	}
	
	
	public List<SubscriptionType> getAll() {
    	List<SubscriptionType> aoReturnList = new ArrayList<SubscriptionType>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find();        	
        	
        	fillList(aoReturnList, oWSDocument, SubscriptionType.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	RiseLog.errorLog("SubscriptionTypeRepository.getAll: error", oEx);
        }

        return aoReturnList;			
	}
	
	public SubscriptionType getByType(String sType) {
		
		try {
			if (Utils.isNullOrEmpty(sType)) return null;
			
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("stringCode", sType);

	        return (SubscriptionType) get(oCriteria);

		} catch (Exception oEx) {
        	RiseLog.errorLog("SubscriptionTypeRepository.getByType: error", oEx);
        }
		
		return null;
	}
}
