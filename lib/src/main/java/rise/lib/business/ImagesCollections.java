package rise.lib.business;

/**
 * Enum with the name of the different allowed Image Collections in WASDI
 * 
 * @author p.campanella
 *
 */
public enum ImagesCollections {	
	EVENTS_IMAGES("event_images"),
	EVENTS_DOCS("event_docs");
	
	private String m_sFolder;
	
	ImagesCollections(String sFolder) {
		this.m_sFolder=sFolder;
	}
	public String getFolder() {
		return m_sFolder;
	}
	
}
