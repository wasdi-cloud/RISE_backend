package rise.lib.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import rise.lib.business.Map;
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
	        Bson oProjection = Projections.include("id", "dateFiltered", "maxAgeDays");

	        // Use the cursor directly. Avoid converting to 'Map' class via Reflection/Jackson
	        MongoCursor<Document> cursor = getCollection(m_sThisCollection)
	                                        .find(Filters.in("id", asMapIds))
	                                        .projection(oProjection)
	                                        .iterator();
	        
	        try {
	            while (cursor.hasNext()) {
	                Document doc = cursor.next();
	                
	                // Manual fast mapping
	                Map mapConfig = new Map();
	                mapConfig.setId(doc.getString("id"));
	                
	                // Handle potential nulls safely
	                Boolean dateFiltered = doc.getBoolean("dateFiltered");
	                mapConfig.setDateFiltered(dateFiltered != null ? dateFiltered : false);
	                
	                Integer maxAge = doc.getInteger("maxAgeDays");
	                mapConfig.setMaxAgeDays(maxAge != null ? maxAge : -1);
	                
	                aoMapsById.put(mapConfig.getId(), mapConfig);
	            }
	        } finally {
	            cursor.close();
	        }
	        
	    } catch (Exception oEx) {
	        RiseLog.errorLog("MapRepository.getMapsByIds: error", oEx);
	    }

	    return aoMapsById;
	}
	
}
