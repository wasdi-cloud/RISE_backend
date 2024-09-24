package rise.lib.business;

public class Map extends RiseEntity {
	
	private String name;
	
	private String description;
	
	private String layerBaseName;
	
	private String icon;

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
	
	
}
