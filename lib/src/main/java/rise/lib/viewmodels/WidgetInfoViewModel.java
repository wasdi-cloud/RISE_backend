package rise.lib.viewmodels;

import java.util.HashMap;

public class WidgetInfoViewModel extends RiseViewModel {

	public String id;
	public String organizationId;
	public String areaId;
	public String areaName;
	
	public String widget;
	public String bbox;
	public String type;
	public String icon;
	public String title;
	public String content;
	public double referenceTime;
	
	public HashMap<String, Object> payload = new HashMap<>();
}
