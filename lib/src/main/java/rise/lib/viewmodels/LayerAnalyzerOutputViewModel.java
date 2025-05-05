package rise.lib.viewmodels;

import java.util.ArrayList;
import java.util.List;

public class LayerAnalyzerOutputViewModel extends RiseViewModel {
	
	public String totAreaPixels;
	
	public String percentTotAreaAffectedPixels;
	public String percentAreaAffectedPixels;
	public String areaPixelAffected;
	
	public String estimatedArea;
	public String estimatedAffectedArea;
	public List<String> histogram = new ArrayList<>();
}
