package rise.lib.business;

import java.util.HashMap;

public class WidgetInfo extends RiseEntity {
	
	private String id;
	private String organizationId;
	private String areaId;
	private String widget;
	private String bbox;
	private String type;
	private String icon;
	private String title;
	private String content;
	private double referenceTime;
	private String referenceDate;
	
	private HashMap<String, Object> payload = new HashMap<>();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOrganizationId() {
		return organizationId;
	}
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}
	public String getWidget() {
		return widget;
	}
	public void setWidget(String widget) {
		this.widget = widget;
	}
	public String getBbox() {
		return bbox;
	}
	public void setBbox(String bbox) {
		this.bbox = bbox;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public HashMap<String, Object> getPayload() {
		return payload;
	}
	public void setPayload(HashMap<String, Object> payload) {
		this.payload = payload;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public double getReferenceTime() {
		return referenceTime;
	}
	public void setReferenceTime(double referenceTime) {
		this.referenceTime = referenceTime;
	}
	public String getAreaId() {
		return areaId;
	}
	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}
	public String getReferenceDate() {
		return referenceDate;
	}
	public void setReferenceDate(String referenceDate) {
		this.referenceDate = referenceDate;
	}

}
