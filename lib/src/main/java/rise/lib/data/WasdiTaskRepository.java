package rise.lib.data;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;


import rise.lib.utils.log.RiseLog;

public class WasdiTaskRepository extends MongoRepository {
	
	public WasdiTaskRepository() {
		m_sThisCollection = "wasdi_tasks";
	}
	
	public long deleteByAreaId(String sAreaId) {
		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.eq("areaId", sAreaId));
			
			return oDeleteResult.getDeletedCount();			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("WasdiTaskRepository.deleteByAreaId exception: " + oEx.toString());
		}
		
		return 0L;
	}
}
