package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.Subscription;
import rise.lib.utils.log.RiseLog;

public class SubscriptionRepository extends MongoRepository {
	
	public SubscriptionRepository() {
		m_sThisCollection = "subscriptions";
		m_oEntityClass = Subscription.class;
	}
	
	public List<Subscription> getSubscriptionsByOrganizationId(String sOrganizationId) {
    	List<Subscription> aoReturnList = new ArrayList<Subscription>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("organizationId", sOrganizationId));        	
        	
        	fillList(aoReturnList, oWSDocument, Subscription.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	RiseLog.errorLog("SubscriptionRepository.getSubscriptionsByOrganizationId: error", oEx);
        }

        return aoReturnList;			
	}
}
