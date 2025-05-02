package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.Area;
import rise.lib.utils.log.RiseLog;

public class AreaRepository extends MongoRepository {
	
	public AreaRepository() {
		m_sThisCollection = "areas";
		m_oEntityClass = Area.class;
	}
	
	public List<Area> getByOrganization(String sOrganizationId) {
    	List<Area> aoReturnList = new ArrayList<Area>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("organizationId", sOrganizationId));        	
        	
        	fillList(aoReturnList, oWSDocument, Area.class);
        	
        	return aoReturnList;
        	
        } 
        catch (Exception oEx) {
        	RiseLog.errorLog("AreaRepository.getByOrganization: error", oEx);
        }

        return aoReturnList;			
	}
	
	public List<Area> getPublicAreas() {
    	List<Area> aoReturnList = new ArrayList<Area>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("publicArea", true));        	
        	
        	fillList(aoReturnList, oWSDocument, Area.class);
        	
        	return aoReturnList;
        	
        } 
        catch (Exception oEx) {
        	RiseLog.errorLog("AreaRepository.getPublicAraes: error", oEx);
        }

        return aoReturnList;			
	}
}
