package rise.lib.business;

import java.util.HashMap;

public class Layer extends RiseEntity {
	
	private String layerId;
	
	private String geoserverUrl;
	
	private Double referenceDate;
	
	private String source;
	
	private HashMap<String, String> properties = new HashMap<>();
	
	private String mapId;
	
	private String pluginId;
	
	private String areaId;
	
	private String id;
	
	private boolean published=false;
	
	private boolean keepLayer=false;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Double getReferenceDate() {
		return referenceDate;
	}

	public void setReferenceDate(Double referenceDate) {
		this.referenceDate = referenceDate;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public HashMap<String, String> getProperties() {
		return properties;
	}

	public void setProperties(HashMap<String, String> properties) {
		this.properties = properties;
	}

	public String getMapId() {
		return mapId;
	}

	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}

	public String getLayerId() {
		return layerId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public String getGeoserverUrl() {
		return geoserverUrl;
	}

	public void setGeoserverUrl(String geoserverUrl) {
		this.geoserverUrl = geoserverUrl;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public boolean isKeepLayer() {
		return keepLayer;
	}

	public void setKeepLayer(boolean keepLayer) {
		this.keepLayer = keepLayer;
	}
}

