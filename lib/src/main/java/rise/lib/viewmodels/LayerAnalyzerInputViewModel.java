package rise.lib.viewmodels;

import java.util.ArrayList;
import java.util.List;

public class LayerAnalyzerInputViewModel extends RiseViewModel {
	public List<String> layerIds = new ArrayList<>();
	public String bbox;
	public String outputPath = "";
	public String filter;
	public String areaId;
	public String mapId;
	public String pluginId;
}
