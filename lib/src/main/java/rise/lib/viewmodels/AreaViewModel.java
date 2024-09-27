package rise.lib.viewmodels;

import java.util.ArrayList;

public class AreaViewModel extends RiseViewModel  {

	/**
	 * Default Id
	 */
	public String id;
	
	public String name;
	
	public String description;
		
	public Double creationDate;
	
	public String subscriptionId;
	
	public String bbox;
	
	public String markerCoordinates;
	
	public String shapeFileMask;
	
	public boolean supportArchive;
	
	public Double archiveStartDate;
	
	public Double archiveEndDate;
	
	public ArrayList<String> plugins = new ArrayList<>();
}
