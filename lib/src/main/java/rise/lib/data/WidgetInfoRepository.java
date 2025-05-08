package rise.lib.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import rise.lib.business.WidgetInfo;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;

public class WidgetInfoRepository extends MongoRepository {

	public WidgetInfoRepository() {
		m_sThisCollection = "widget_infos";
		this.m_oEntityClass = WidgetInfo.class;
	}
	
	public List<WidgetInfo> getByWidgetOrganizationId(String sWidget, String sOrganizationId) {
    	List<WidgetInfo> aoReturnList = new ArrayList<WidgetInfo>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("widget", sWidget), Filters.eq("organizationId", sOrganizationId)));        	
        	
        	fillList(aoReturnList, oWSDocument, WidgetInfo.class);
        	
        	return aoReturnList;
        	
        } catch (Exception oEx) {
        	RiseLog.errorLog("WidgetInfoRepository.getByOrganizationId: error", oEx);
        }

        return aoReturnList;			
	}
	
	public WidgetInfo getByWidgetOrganizationIdTime(String sWidget, String sOrganizationId, double dTime) {
		
		try {
			DBObject oSort= new BasicDBObject();
			oSort.put("referenceTime", -1);
			Document oSortDoc = new Document(oSort.toMap());
			Document oDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("widget", sWidget), Filters.eq("organizationId", sOrganizationId), Filters.lte("referenceTime", dTime))).sort(oSortDoc).first();
			
            if(oDocument == null)
            {
            	return null;
            }
            
            String sJSON = oDocument.toJson();

            WidgetInfo oEntity = (WidgetInfo) s_oMapper.readValue(sJSON, m_oEntityClass);

            return oEntity;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("WidgetInfoRepository.getByWidgetOrganizationIdTime exception: " + oEx.toString());
		}
		
		return null;
	}
	
	public List<WidgetInfo> getListByWidgetOrganizationIdForDay(String sWidget, String sOrganizationId, Date oDay) {

		List<WidgetInfo> aoReturnList = new ArrayList<WidgetInfo>();
		try {
			
			double dStartDay = DateUtils.getBeginningOfDayTimestamp(oDay)*1000;
			double dEndDay = DateUtils.getEndOfDayTimestamp(oDay)*1000;
			
			
			DBObject oSort= new BasicDBObject();
			oSort.put("referenceTime", -1);
			Document oSortDoc = new Document(oSort.toMap());
			FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("widget", sWidget), Filters.eq("organizationId", sOrganizationId), Filters.lte("referenceTime", dEndDay), Filters.gte("referenceTime", dStartDay) )).sort(oSortDoc);
			fillList(aoReturnList, oWSDocument, WidgetInfo.class);
			

            return aoReturnList;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("WidgetInfoRepository.getByWidgetOrganizationIdTime exception: " + oEx.toString());
		}
		
		return aoReturnList;
	}	
		
}
