package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.MapsParameters;
import rise.lib.business.Plugin;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

public class PluginRepository extends MongoRepository {
	
	public PluginRepository() {
		m_sThisCollection = "plugins";
		m_oEntityClass = Plugin.class;
	}
	
	public List<Plugin> getAll() {
    	List<Plugin> aoReturnList = new ArrayList<Plugin>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find();        	
        	
        	fillList(aoReturnList, oWSDocument, Plugin.class);
        	
        	return aoReturnList;
        	
        } 
        catch (Exception oEx) {
        	RiseLog.errorLog("PluginRepository.getAll: error", oEx);
        }

        return aoReturnList;			
	}
	
	public List<Plugin> listById(List<String> asPluginIds) {
		
		List<Plugin> aoRes = null;
		
		if (asPluginIds == null || asPluginIds.size() == 0) {
			RiseLog.warnLog("PluginRepository.listById: list of plugin ids is null or empty");
			return aoRes;
		}
		
		try {
			
			Bson oFilter = Filters.in("id", asPluginIds);
			
			FindIterable<Document> aoDocument = getCollection(m_sThisCollection).find(oFilter);
			
			aoRes = new ArrayList<Plugin>();
			
			fillList(aoRes, aoDocument, Plugin.class);
	        
	        return aoRes;	
			
		} catch (Exception oE) {
			RiseLog.errorLog("PluginRepository.listById: exception", oE);
		}
		
		return null;
	}
	
	
	/**
	 * Check if a map is part of a plugin
	 * @param: 
	 */
	public boolean hasMap(String sPluginId, String sMapId) {
		
		if (Utils.isNullOrEmpty(sPluginId)) {
			RiseLog.warnLog("PluginRepository.hasMap: plugin id is null or empty");
			return false;
		}
		
		if (Utils.isNullOrEmpty(sMapId)) {
			RiseLog.warnLog("PluginRepository.hasMap: map id is null or empty");
			return false;
		}
				
		try {
	    	List<Plugin> aoReturnList = new ArrayList<Plugin>();
			
			Document oQuery = new Document()
					.append("id", sPluginId)
					.append("maps", sMapId);
			
			Document oDocument = getCollection(m_sThisCollection).find(oQuery).first();
			
			return oDocument != null;
		
		}
		catch (Exception oEx) {
			RiseLog.errorLog("PluginRepository.hasMap: error", oEx);
		}
		
		return false;
		
	}
	
	
	public Plugin getPluginFromMapId(String sMapId) {
		
		if (Utils.isNullOrEmpty(sMapId)) {
			RiseLog.warnLog("PluginRepository.hasMap: map id is null or empty");
			return null;
		}
				
		try {			
			Document oQuery = new Document().append("maps", sMapId);
			
			Document oDocument = getCollection(m_sThisCollection).find(oQuery).first();
			
	        String sJSON = oDocument.toJson();
	        
	        Plugin  oPlugin = (Plugin) s_oMapper.readValue(sJSON, m_oEntityClass);  
	        
	        return oPlugin;	
		}
		catch (Exception oEx) {
			RiseLog.errorLog("PluginRepository.hasMap: error", oEx);
			return null;
		}
		
	}
	
}
