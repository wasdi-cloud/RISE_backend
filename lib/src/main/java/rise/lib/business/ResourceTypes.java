package rise.lib.business;

public enum ResourceTypes {
	AREA("area");
	
	private final String resourceType;

	ResourceTypes(String sResourceType) {
		this.resourceType = sResourceType;
	}

	public String getResourceType() {
		return resourceType;
	}	
}
