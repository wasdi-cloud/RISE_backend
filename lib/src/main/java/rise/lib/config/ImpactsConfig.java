package rise.lib.config;

import java.util.ArrayList;

public class ImpactsConfig {
	public String impactPlugin = "rise_impact_plugin";
	public ArrayList<String> impactFeaturesMaps = new ArrayList<>() {{
		add("roads");
		add("exposures");
	}}; 

}
