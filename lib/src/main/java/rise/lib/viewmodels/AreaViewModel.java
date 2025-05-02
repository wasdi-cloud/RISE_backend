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
	
	public boolean active = true;
	
	public ArrayList<String> plugins = new ArrayList<>();
	
	public boolean firstShortArchivesReady = false;
	
	public boolean allShortArchivesReady = false;

	public boolean firstFullArchivesReady = false;
	
	public boolean allFullArchivesReady = false;
	
	public boolean isPublicArea;
}
