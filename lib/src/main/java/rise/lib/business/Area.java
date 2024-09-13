package rise.lib.business;

import java.util.ArrayList;

public class Area {
	
	private String id;
	
	private String name;
	
	private String description;
	
	private ArrayList<String> plugins;
	
	private ArrayList<String> fieldOperators;
	
	private Double creationDate;
	
	private String subscriptionId;
	
	private String bbox;
	
	private String markerCoordinates;
	
	private String shapeFileMask;
	
	private boolean supportArchive;
	
	private Double archiveStartDate;
	
	private Double archiveEndDate;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<String> getPlugins() {
		return plugins;
	}

	public void setPlugins(ArrayList<String> plugins) {
		this.plugins = plugins;
	}

	public ArrayList<String> getFieldOperators() {
		return fieldOperators;
	}

	public void setFieldOperators(ArrayList<String> fieldOperators) {
		this.fieldOperators = fieldOperators;
	}

	public Double getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Double creationDate) {
		this.creationDate = creationDate;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getBbox() {
		return bbox;
	}

	public void setBbox(String bbox) {
		this.bbox = bbox;
	}

	public String getMarkerCoordinates() {
		return markerCoordinates;
	}

	public void setMarkerCoordinates(String markerCoordinates) {
		this.markerCoordinates = markerCoordinates;
	}

	public String getShapeFileMask() {
		return shapeFileMask;
	}

	public void setShapeFileMask(String shapeFileMask) {
		this.shapeFileMask = shapeFileMask;
	}

	public boolean isSupportArchive() {
		return supportArchive;
	}

	public void setSupportArchive(boolean supportArchive) {
		this.supportArchive = supportArchive;
	}

	public Double getArchiveStartDate() {
		return archiveStartDate;
	}

	public void setArchiveStartDate(Double archiveStartDate) {
		this.archiveStartDate = archiveStartDate;
	}

	public Double getArchiveEndDate() {
		return archiveEndDate;
	}

	public void setArchiveEndDate(Double archiveEndDate) {
		this.archiveEndDate = archiveEndDate;
	}
	

}
