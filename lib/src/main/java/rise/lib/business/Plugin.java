package rise.lib.business;

import java.util.ArrayList;

public class Plugin extends RiseEntity {
	
	private String name;
	
	private String shortDescription;
	
	private String longDescription;
	
	private boolean supportArchive;
	
	private Double archivePrice;
	
	private Double emergencyPrice;
	
	private String stringCode;
	
	private ArrayList<String> maps = new ArrayList<>();

	private String id;
	
	private String className;

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

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	public boolean isSupportArchive() {
		return supportArchive;
	}

	public void setSupportArchive(boolean supportArchive) {
		this.supportArchive = supportArchive;
	}

	public Double getArchivePrice() {
		return archivePrice;
	}

	public void setArchivePrice(Double archivePrice) {
		this.archivePrice = archivePrice;
	}

	public Double getEmergencyPrice() {
		return emergencyPrice;
	}

	public void setEmergencyPrice(Double emergencyPrice) {
		this.emergencyPrice = emergencyPrice;
	}

	public ArrayList<String> getMaps() {
		return maps;
	}

	public void setMaps(ArrayList<String> maps) {
		this.maps = maps;
	}

	public String getStringCode() {
		return stringCode;
	}

	public void setStringCode(String stringCode) {
		this.stringCode = stringCode;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
}
