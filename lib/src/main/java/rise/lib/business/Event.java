package rise.lib.business;

public class Event extends RiseEntity {

	private String name;
	
	private EventType type;
	
	private String bbox;
	
	private String markerCoordinates;
	
	private double startDate;
	
	private double endDate;
	
	private double peakDate;
	
	private String areaId;
	
	private String id;
	
	private String description;
	
	private boolean isInGoing;
	
	private boolean isPublic;

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

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public String getBbox() {
		return bbox;
	}

	public void setBbox(String bbox) {
		this.bbox = bbox;
	}

	public double getStartDate() {
		return startDate;
	}

	public void setStartDate(double startDate) {
		this.startDate = startDate;
	}

	public double getEndDate() {
		return endDate;
	}

	public void setEndDate(double endDate) {
		this.endDate = endDate;
	}

	public double getPeakDate() {
		return peakDate;
	}

	public void setPeakDate(double peakDate) {
		this.peakDate = peakDate;
	}

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the markerCoordinates
	 */
	public String getMarkerCoordinates() {
		return markerCoordinates;
	}

	/**
	 * @param markerCoordinates the markerCoordinates to set
	 */
	public void setMarkerCoordinates(String markerCoordinates) {
		this.markerCoordinates = markerCoordinates;
	}

	/**
	 * @return the isInGoing
	 */
	public boolean isInGoing() {
		return isInGoing;
	}

	/**
	 * @param isInGoing the isInGoing to set
	 */
	public void setInGoing(boolean isInGoing) {
		this.isInGoing = isInGoing;
	}

	/**
	 * @return the isPublic
	 */
	public boolean isPublic() {
		return isPublic;
	}

	/**
	 * @param isPublic the isPublic to set
	 */
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	
}
