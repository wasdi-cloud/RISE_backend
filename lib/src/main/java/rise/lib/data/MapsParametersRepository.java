package rise.lib.data;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import rise.lib.business.MapsParameters;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

public class MapsParametersRepository extends MongoRepository {
	
	public MapsParametersRepository() {
		m_sThisCollection = "maps_parameters";
		m_oEntityClass = MapsParameters.class;
	}
	
	
	public MapsParameters getMapParamer(String sMapParameterId) {
		
		if (Utils.isNullOrEmpty(sMapParameterId))
			return null;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("id", sMapParameterId);

		return (MapsParameters) get(oCriteria);
	}
	
	
	public MapsParameters getMostRecentParameters(String sAreaId, String sPluginId, String sMapId) {
		
		if (Utils.isNullOrEmpty(sAreaId)) {
			RiseLog.warnLog("MapsParametersRepository.getMostRecentParameters: area id is null or empty");
			return null;
		}
		
		if (Utils.isNullOrEmpty(sPluginId)) {
			RiseLog.warnLog("MapsParametersRepository.getMostRecentParameters: plugin id is null or empty");
			return null;
		}
		
		if (Utils.isNullOrEmpty(sMapId)) {
			RiseLog.warnLog("MapsParametersRepository.getMostRecentParameters: map id is null or empty");
			return null;
		}
		
	    Bson oFilter = Filters.and(
	            Filters.eq("areaId", sAreaId),
	            Filters.eq("pluginId", sPluginId),
	            Filters.eq("mapId", sMapId)
	        );
		
	    try {
		    Document oResult =  getCollection(m_sThisCollection).find(oFilter)
		            .sort(Sorts.descending("timestamp"))
		            .limit(1)
		            .first();
		    
	        String sJSON = oResult.toJson();
	        MapsParameters oMostRecentParameters = (MapsParameters) s_oMapper.readValue(sJSON, m_oEntityClass);  
	        return oMostRecentParameters;
        
	    } catch (Exception oEx) {
        	RiseLog.errorLog("MapsParametersRepository.getMostRecentParameters: exception " + oEx);
        	return null;
        }
	}
	
}
