package rise.lib.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import rise.lib.business.Plugin;
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
}
