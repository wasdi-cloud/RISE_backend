package rise.lib.viewmodels;

import java.util.HashMap;

public class LayerViewModel extends RiseViewModel {
	
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
}

