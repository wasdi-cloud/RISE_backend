package rise;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.annotation.PostConstruct;
import rise.providers.JerseyMapperProvider;

public class Rise extends ResourceConfig {
	
	Rise() {
		register(JacksonFeature.class);
		register(JerseyMapperProvider.class);
		
		packages(true, "rise.api");
	}
	
	@PostConstruct
	public void initRise() {
		System.out.println("Welcome to RISE!");
	}
	
	
}
