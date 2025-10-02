package rise.lib.data;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import rise.lib.business.Layer;
import rise.lib.utils.log.RiseLog;

public class LayerRepository extends MongoRepository {
	
	public LayerRepository() {
		m_sThisCollection = "layers";
		m_oEntityClass = Layer.class;
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

	
	public Layer getLayerByArea(String sAreaId) {
		
		try {
			DBObject oSort= new BasicDBObject();
			oSort.put("referenceDate", -1);
			Document oSortDoc = new Document(oSort.toMap());
			Document oDocument = getCollection(m_sThisCollection).find(Filters.eq("areaId", sAreaId)).sort(oSortDoc).first();
			
            if(oDocument == null)
            {
            	return null;
            }
            
            String sJSON = oDocument.toJson();

            Layer oEntity = (Layer) s_oMapper.readValue(sJSON, m_oEntityClass);

            return oEntity;			
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
}
