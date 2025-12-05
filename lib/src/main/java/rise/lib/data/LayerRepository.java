package rise.lib.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

import rise.lib.business.Layer;
import rise.lib.utils.log.RiseLog;

public class LayerRepository extends MongoRepository {
	
	public LayerRepository() {
		m_sThisCollection = "layers";
		m_oEntityClass = Layer.class;
	}

	// Returns: Map<MapID, ReferenceDate (seconds)>
	public java.util.Map<String, Double> getLatestLayerDates(String sAreaId, List<String> asMapIds, double dReferenceTimeSec) {
	    java.util.Map<String, Double> result = new HashMap<>();

	    try {
	        // 1. MATCH: Filter documents first (Area, Maps, Time)
	        Bson match = Aggregates.match(Filters.and(
	                Filters.eq("areaId", sAreaId),
	                Filters.in("mapId", asMapIds),
	                Filters.lte("referenceDate", dReferenceTimeSec)
	        ));

	        // 2. SORT: Order by referenceDate DESC so the newest is first
	        Bson sort = Aggregates.sort(Sorts.descending("referenceDate"));

	        // 3. GROUP: Group by MapId, taking the first (newest) date found
	        Bson group = Aggregates.group("$mapId", Accumulators.first("latestDate", "$referenceDate"));

	        // 4. Run the pipeline
	        List<Bson> pipeline = Arrays.asList(match, sort, group);

	        // Execute Aggregation
	        // This is FAST because we don't map to Java Objects (Layer class)
	        for (Document doc : getCollection(m_sThisCollection).aggregate(pipeline)) {
	            String mapId = doc.getString("_id"); // _id is the groupBy key (mapId)
	            Double date = doc.getDouble("latestDate");
	            
	            if (mapId != null && date != null) {
	                result.put(mapId, date);
	            }
	        }
	    } catch (Exception oEx) {
	        RiseLog.errorLog("LayerRepository.getLatestLayerDates error: " + oEx);
	    }
	    
	    return result;
	}
	
	// Returns: Map<MapID, ReferenceDate (seconds)>
	public java.util.Map<String, Layer> getLatestLayerDates2(String sAreaId, List<String> asMapIds, double dReferenceTimeSec) {
	    java.util.Map<String, Layer> aoResults = new HashMap<>();

	    try {
	        // 1. MATCH: Filter documents first (Area, Maps, Time)
	        Bson match = Aggregates.match(Filters.and(
	                Filters.eq("areaId", sAreaId),
	                Filters.in("mapId", asMapIds),
	                Filters.lte("referenceDate", dReferenceTimeSec)
	        ));

	        // 2. SORT: Order by referenceDate DESC so the newest is first
	        Bson sort = Aggregates.sort(Sorts.descending("referenceDate"));

	        // 3. GROUP: Group by MapId, taking the first (newest) date found
	        Bson group = Aggregates.group("$mapId", Accumulators.first("latestDate", "$referenceDate"));

	        // 4. Run the pipeline
	        List<Bson> pipeline = Arrays.asList(match, sort, group);

	        // Execute Aggregation
	        // This is FAST because we don't map to Java Objects (Layer class)
	        for (Document oDoc : getCollection(m_sThisCollection).aggregate(pipeline)) {
	            String sMapMongoId = oDoc.getString("_id"); // _id is the groupBy key (mapId)
	            Double oLatestDate = oDoc.getDouble("latestDate");
	            
	            if (sMapMongoId != null && oLatestDate != null) {
		            String sJSON = oDoc.toJson();
		            Layer oEntity = (Layer) s_oMapper.readValue(sJSON, m_oEntityClass);
		            aoResults.put(sMapMongoId, oEntity);
	            }
	        }
	    } catch (Exception oEx) {
	        RiseLog.errorLog("LayerRepository.getLatestLayerDates2 error: " + oEx);
	    }
	    
	    return aoResults;
	}
		
	
	public Layer getLayerByAreaMapTime(String sAreaId, String sMapId, double dTime) {
		
		try {
			DBObject oSort= new BasicDBObject();
			oSort.put("referenceDate", -1);
			Document oSortDoc = new Document(oSort.toMap());
			Document oDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("areaId", sAreaId), Filters.eq("mapId", sMapId), Filters.lte("referenceDate", dTime))).sort(oSortDoc).first();
			
            if(oDocument == null)
            {
            	return null;
            }
            
            String sJSON = oDocument.toJson();

            Layer oEntity = (Layer) s_oMapper.readValue(sJSON, m_oEntityClass);

            return oEntity;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("LayerRepository.getLayerByAreaMapTime exception: " + oEx.toString());
		}
		
		return null;
	}
	
	public Layer getLayerByAreaMap(String sAreaId, String sMapId) {
		
		try {
			DBObject oSort= new BasicDBObject();
			oSort.put("referenceDate", -1);
			Document oSortDoc = new Document(oSort.toMap());
			Document oDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("areaId", sAreaId), Filters.eq("mapId", sMapId))).sort(oSortDoc).first();
			
            if(oDocument == null)
            {
            	return null;
            }
            
            String sJSON = oDocument.toJson();

            Layer oEntity = (Layer) s_oMapper.readValue(sJSON, m_oEntityClass);

            return oEntity;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("LayerRepository.getLayerByAreaMapTime exception: " + oEx.toString());
		}
		
		return null;
	}

	
	public List<Layer> getLayerByArea(String sAreaId) {
		
		try {

			List<Layer> aoReturnList = new ArrayList<Layer>();

			try {

				FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("areaId", sAreaId));

				fillList(aoReturnList, oWSDocument, Layer.class);

				return aoReturnList;

			} catch (Exception oEx) {
				RiseLog.errorLog("LayerRepository.getLayerByAreaMapTime: error", oEx);
			}

			return aoReturnList;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("LayerRepository.getLayerByArea exception: " + oEx.toString());
		}
		
		return null;
	}

	public long deleteByAreaId(String sAreaId) {
		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.eq("areaId", sAreaId));
			
			return oDeleteResult.getDeletedCount();			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("LayerRepository.deleteByAreaId exception: " + oEx.toString());
		}
		
		return 0L;
	}
	
	
	// Add the dTime parameter to the repository method signature
	public List<Layer> getLayersByAreaAndMapIds(String sAreaId, List<String> asMapIds, double dTime) {
	    List<Layer> aoReturnList = new ArrayList<>();

	    if (asMapIds == null || asMapIds.isEmpty()) {
	        return aoReturnList;
	    }

	    try {
	        // 1. Create the base filters
	        Bson oAreaFilter = Filters.eq("areaId", sAreaId);
	        Bson oMapIdFilter = Filters.in("mapId", asMapIds); 
	        
	        // 2. Add a crucial date filter: Layers must be older than or equal to the requested time
	        // This covers the Filters.lte("referenceDate", dTime) logic for ALL maps.
	        Bson oTimeFilter = Filters.lte("referenceDate", dTime); 

	        // 3. Combine ALL filters 
	        Bson oCombinedFilter = Filters.and(oAreaFilter, oMapIdFilter, oTimeFilter);
	        
	        // 4. IMPORTANT: Sort the result! 
	        // We MUST return the layers sorted by date descending so that the latest layer for each
	        // map ID comes first when processing in-memory.
	        DBObject oSort= new BasicDBObject();
	        oSort.put("referenceDate", -1);
	        Document oSortDoc = new Document(oSort.toMap());

	        // 5. Execute the query
	        FindIterable<Document> oWSDocument = getCollection(m_sThisCollection)
	                                                .find(oCombinedFilter)
	                                                .sort(oSortDoc); // Apply the sort here!
	        
	        fillList(aoReturnList, oWSDocument, Layer.class);
	        
	    }  catch (Exception oEx) {
	        // Log the error
	        RiseLog.errorLog("LayerRepository.getLayersByAreaAndMapIds: error", oEx);
	    }

	    return aoReturnList;
	}
	
}
