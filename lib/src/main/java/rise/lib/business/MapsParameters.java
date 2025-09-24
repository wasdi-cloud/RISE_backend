package rise.lib.business;

public class MapsParameters extends RiseEntity {
	
	private String id;
	private String areaId;
	private String pluginId;
	private String mapId;
	private String payload;
	private String creationUserId;
	private long creationTimestamp;
	private String lastModifyUserId;
	private long lastModifyTimestamp;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getAreaId() {
		return areaId;
	}
	
	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}
	
	public String getPluginId() {
		return pluginId;
	}
	
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}
	
	public String getMapId() {
		return mapId;
	}
	
	public void setMapId(String mapId) {
		this.mapId = mapId;
	}
	
	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	public String getCreationUserId() {
		return creationUserId;
	}

	public void setCreationUserId(String creationUserId) {
		this.creationUserId = creationUserId;
	}
	
	public long getCreationTimestamp() {
		return creationTimestamp;
	}
	
	public void setCreationTimestamp(long creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	public String getLastModifyUserId() {
		return lastModifyUserId;
	}

	public void setLastModifyUserId(String lastModifyUserId) {
		this.lastModifyUserId = lastModifyUserId;
	}

	public long getLastModifyTimestamp() {
		return lastModifyTimestamp;
	}

	public void setLastModifyTimestamp(long lastModifyTimestamp) {
		this.lastModifyTimestamp = lastModifyTimestamp;
	}
}
