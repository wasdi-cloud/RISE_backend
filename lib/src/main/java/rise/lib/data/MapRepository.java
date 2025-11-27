package rise.lib.data;

import java.util.HashMap;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import rise.lib.business.Map;
import rise.lib.business.Plugin;
import rise.lib.utils.log.RiseLog;

public class MapRepository extends MongoRepository {
	
	public MapRepository() {
		m_sThisCollection = "maps";
		m_oEntityClass = Map.class;
	}
	
	
	public java.util.Map<String, Map> getMapsByIds(List<String> asMapIds) {
		
	    if (asMapIds == null || asMapIds.isEmpty()) return new HashMap<>();
	    
	    java.util.Map<String, Map> aoMapsById = new HashMap<>();

	    try {

	        // Use the cursor directly. Avoid converting to 'Map' class via Reflection/Jackson
	        MongoCursor<Document> oCursor = getCollection(m_sThisCollection)
	                                        .find(Filters.in("id", asMapIds))
	                                        .iterator();
	        
	        try {
	            while (oCursor.hasNext()) {
	                Document oDoc = oCursor.next();
	                
	    	        String sJSON = oDoc.toJson();
	    	        
	    	        Map oMapEntity = (Map) s_oMapper.readValue(sJSON, m_oEntityClass);
	                
	                aoMapsById.put(oMapEntity.getId(), oMapEntity);
	            }
	        } finally {
	            oCursor.close();
	        }
	        
	    } catch (Exception oEx) {
	        RiseLog.errorLog("MapRepository.getMapsByIds: error", oEx);
	    }

	    return aoMapsById;
	}
	
}
