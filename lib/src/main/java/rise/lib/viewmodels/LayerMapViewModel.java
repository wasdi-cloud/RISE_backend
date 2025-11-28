package rise.lib.viewmodels;

import java.util.HashMap;

public class LayerMapViewModel {
	public String layerId;
	
	public Double referenceDate;
	
	public String source;
	
	public HashMap<String, String> properties = new HashMap<>();
	
	public String mapId;
	
	public String pluginId;
	
	public String areaId;
	
	public String id;
	
	public String geoserverUrl;
	
	public boolean published = false;
	
	public String dataSource = "";
	
	public Double createdDate = 0.0;
	
	public String resolution = "";
	
	public String inputData = "";
	
	public String name;
	
	public String icon;
	
	public boolean disabled = false;
	
	public String description;
	
	public boolean loaded = false;
}
