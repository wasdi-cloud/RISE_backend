package rise;

import java.io.File;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Context;
import rise.lib.business.Session;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.MongoRepository;
import rise.lib.data.SessionRepository;
import rise.lib.data.UserRepository;
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
		
		RiseLog.debugLog("----------- Welcome to RISE Remote Imaging Support for Emergencies 0.0.2-----------");

		String sConfigFilePath = "/etc/rise/riseConfig.json";
		

		if (Utils.isNullOrEmpty(m_oServletConfig.getInitParameter("ConfigFilePath")) == false){
			String sTestFile = m_oServletConfig.getInitParameter("ConfigFilePath");
			File oTestConfig = new File(sTestFile);
			if (oTestConfig.exists()) {
				sConfigFilePath = m_oServletConfig.getInitParameter("ConfigFilePath");	
			}
			else {
				RiseLog.errorLog("The configured file  " + sTestFile + " does not exists, try to fall back to default " + sConfigFilePath);
			}
		}
		
		 
		
		if (!RiseConfig.readConfig(sConfigFilePath)) {
			RiseLog.debugLog("ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}
		else {
			RiseLog.debugLog("READ CONFIG FILE " + sConfigFilePath);
		}
		
		// Read MongoDb Configuration
		try {

            MongoRepository.readConfig();

            RiseLog.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable oEx) {
			RiseLog.errorLog("Read MongoDb Configuration exception " + oEx.toString());
		}		
	}
	
	
	/**
	 * Get the User object from the session Id
	 * It checks first in Key Cloak and later on the local session mechanism.
	 * @param sSessionId
	 * @return
	 */
	public static User getUserFromSession(String sSessionId) {
		
		if (Utils.isNullOrEmpty(sSessionId));
		
		User oUser = null;
		String sUserId = "";
		
		try {
			
			SessionRepository oSessionRepository = new SessionRepository();
			Session oUserSession = oSessionRepository.getSession(sSessionId);
			
			if(null==oUserSession) {
				return null;
			} else {
				sUserId = oUserSession.getUserId();
			}
			
			if(!Utils.isNullOrEmpty(sUserId)){
				sUserId = sUserId.toLowerCase();
				UserRepository oUserRepository = new UserRepository();
				oUser = oUserRepository.getUser(sUserId);
			} 
			else {
				return null;
			}			

		} catch (Exception oE) {
			RiseLog.errorLog("Rise.getUserFromSession: something bad happened: " + oE);
		}

		return oUser;
	}	
		
	public static void shutDown() {
		RiseLog.debugLog("RISE is closing, bye bye!");
	}
	
	
	
}
