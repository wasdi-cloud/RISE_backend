package rise.lib.business;

public enum EventType {

	FLOOD("Flood"), DROUGHT("Drought"), CONFLICT("Conflict"), EARTHQUAKE("Earthquake"), TSUNAMI("Tsunami"),
	INDUSTRIAL_ACCIDENTS("Insutrial Accidents"), LANDSLIDE("Landslide"), OTHER("Other");

	private final String value;

	EventType(String sValue) {
		this.value = sValue;

	}

	public String getString() {
		return value;
	}

}