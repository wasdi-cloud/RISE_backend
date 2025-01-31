package rise.lib.viewmodels;

import rise.lib.business.EventType;

public class EventViewModel extends RiseViewModel {

	/**
	 * Default Id
	 */
	public String id;
	
	
	public String name;
	
	public EventType type;
	
	public String bbox;
	
	public String markerCoordinates;
	
	public double startDate;
	
	public double endDate;
	
	public double peakDate;
	
	public String description;
	
	public boolean inGoing;
	
	public boolean publicEvent;
}
