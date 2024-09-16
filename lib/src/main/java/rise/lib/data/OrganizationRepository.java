package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.Organization;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

public class OrganizationRepository extends MongoRepository {
	
	public OrganizationRepository() {
		m_sThisCollection = "organizations";
		m_oEntityClass = Organization.class;
	}
	
	public Organization getOrganization(String sId) {
		
		if (Utils.isNullOrEmpty(sId)) return null;
		
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("id", sId);

        return (Organization) get(oCriteria);		
	}
	
	public List<Organization> getOrganizationsByName(String sName) {
		
    	List<Organization> aoReturnList = new ArrayList<Organization>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("name", sName));        	
        	
        	fillList(aoReturnList, oWSDocument, Organization.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	RiseLog.errorLog("OrganizationRepository.getOrganizationsByName: error", oEx);
        }

        return aoReturnList;	
	}	
		
}
