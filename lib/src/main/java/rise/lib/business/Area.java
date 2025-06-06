package rise.lib.business;

import java.util.ArrayList;

public class Area extends RiseEntity {
	
	private String name;
	
	private String description;
	
	private ArrayList<String> plugins;
	
	private ArrayList<String> fieldOperators;
	
	private Double creationDate;
	
	private String subscriptionId;
	
	private String organizationId;
	
	private String bbox;
	
	private String markerCoordinates;
	
	private String shapeFileMask;
	
	private boolean supportArchive;
	
	private Double archiveStartDate;
	
	private Double archiveEndDate;
	
	private String id;
	
	private boolean newCreatedArea = true;
	
	private boolean active = true;
	
	private boolean firstShortArchivesReady = false;
	
	private boolean allShortArchivesReady = false;

	private boolean firstFullArchivesReady = false;
	
	private boolean allFullArchivesReady = false;
	
	private boolean publicArea = false;

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

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public boolean isNewCreatedArea() {
		return newCreatedArea;
	}

	public void setNewCreatedArea(boolean newCreatedArea) {
		this.newCreatedArea = newCreatedArea;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isFirstShortArchivesReady() {
		return firstShortArchivesReady;
	}

	public void setFirstShortArchivesReady(boolean firstShortArchivesReady) {
		this.firstShortArchivesReady = firstShortArchivesReady;
	}

	public boolean isAllShortArchivesReady() {
		return allShortArchivesReady;
	}

	public void setAllShortArchivesReady(boolean allShortArchivesReady) {
		this.allShortArchivesReady = allShortArchivesReady;
	}

	public boolean isFirstFullArchivesReady() {
		return firstFullArchivesReady;
	}

	public void setFirstFullArchivesReady(boolean firstFullArchivesReady) {
		this.firstFullArchivesReady = firstFullArchivesReady;
	}

	public boolean isAllFullArchivesReady() {
		return allFullArchivesReady;
	}

	public void setAllFullArchivesReady(boolean allFullArchivesReady) {
		this.allFullArchivesReady = allFullArchivesReady;
	}

	public boolean isPublicArea() {
		return publicArea;
	}

	public void setPublicArea(boolean isPublicArea) {
		this.publicArea = isPublicArea;
	}
	

}
