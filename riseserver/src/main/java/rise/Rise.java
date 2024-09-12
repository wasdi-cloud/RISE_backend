package rise;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Context;
import rise.lib.config.RiseConfig;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.providers.JerseyMapperProvider;


public class Rise extends ResourceConfig {
	
	@Context
	ServletConfig m_oServletConfig;	
	
	public Rise() {
		register(JacksonFeature.class);
		register(JerseyMapperProvider.class);
		
		packages(true, "rise.api");
	}
	
	@PostConstruct
	public void initRise() {
		
		RiseLog.debugLog("----------- Welcome to WASDI - Web Advanced Space Developer Interface");

		String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";

		if (Utils.isNullOrEmpty(m_oServletConfig.getInitParameter("ConfigFilePath")) == false){
			sConfigFilePath = m_oServletConfig.getInitParameter("ConfigFilePath");
		}
		
		if (!RiseConfig.readConfig(sConfigFilePath)) {
			RiseLog.debugLog("ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}
		else {
			RiseLog.debugLog("READ CONFIG FILE " + sConfigFilePath);
		}
	}
	
		
	public static void shutDown() {
		RiseLog.debugLog("RISE is closing, bye bye!");
	}
	
}
