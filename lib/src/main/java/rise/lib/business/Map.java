package rise.lib.business;

public class Map extends RiseEntity {
	
	private String name;
	
	private String description;
	
	private String layerBaseName;
	
	private String icon;
	
	private String id;
	
	private boolean dateFiltered = true;
	
	private String className;
	
	private boolean hidden = false;

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

	public String getLayerBaseName() {
		return layerBaseName;
	}

	public void setLayerBaseName(String layerBaseName) {
		this.layerBaseName = layerBaseName;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public boolean isDateFiltered() {
		return dateFiltered;
	}

	public void setDateFiltered(boolean dateFiltered) {
		this.dateFiltered = dateFiltered;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	
}
