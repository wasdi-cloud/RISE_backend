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
	public List<WidgetInfo> getWidgetListByAreaId(String sAreaId) {
    	List<WidgetInfo> aoReturnList = new ArrayList<WidgetInfo>();

        try {

        	FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("areaId", sAreaId));

        	fillList(aoReturnList, oWSDocument, WidgetInfo.class);

        	return aoReturnList;

        } catch (Exception oEx) {
        	RiseLog.errorLog("WidgetInfoRepository.getWidgetListByAreaId: error", oEx);
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
			
			double dStartDay = DateUtils.getBeginningOfDayTimestamp(oDay);
			double dEndDay = DateUtils.getEndOfDayTimestamp(oDay);
			
			
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
	
	
	public List<WidgetInfo> getListByTypeAreaIdForDay(String sWidget, String sAreaId, String sDate) {

		List<WidgetInfo> aoReturnList = new ArrayList<WidgetInfo>();
		try {
						
			DBObject oSort= new BasicDBObject();
			oSort.put("referenceTime", -1);
			Document oSortDoc = new Document(oSort.toMap());
			FindIterable<Document> oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("widget", sWidget), Filters.eq("areaId", sAreaId), Filters.eq("referenceDate", sDate)  )).sort(oSortDoc);
			fillList(aoReturnList, oWSDocument, WidgetInfo.class);

            return aoReturnList;			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("WidgetInfoRepository.getListByTypeAreaIdForDay exception: " + oEx.toString());
		}
		
		return aoReturnList;
	}
	
	public List<WidgetInfo> getImpactsForAreaDay(String sAreaId, String sDate) {
		List<WidgetInfo> aoReturnList = new ArrayList<WidgetInfo>();
		
		try {
			aoReturnList = getListByTypeAreaIdForDay("impacts_baresoil", sAreaId, sDate);
			List<WidgetInfo> aoTempList = getListByTypeAreaIdForDay("impacts_urban", sAreaId, sDate);
			aoReturnList.addAll(aoTempList);			
		}
		catch (Exception oEx) {
			RiseLog.errorLog("WidgetInfoRepository.getImpactsForAreaDay exception: " + oEx.toString());
		}
		
		return aoReturnList;
		
	}
			
		
}
