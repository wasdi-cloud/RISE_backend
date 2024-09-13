package rise.lib.business;

public class Event {
	
	private String id;

	private String name;
	
	private String type;
	
	private String bbox;
	
	private double startDate;
	
	private double endDate;
	
	private double peakDate;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
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
	
}