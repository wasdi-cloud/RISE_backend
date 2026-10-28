package rise;

import java.io.File;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
import rise.lib.utils.PasswordAuthentication;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.providers.JerseyMapperProvider;


public class Rise extends ResourceConfig {
	
	@Context
	ServletConfig m_oServletConfig;	
	
	public Rise() {
		register(JacksonFeature.class);
		register(JerseyMapperProvider.class);
		register(MultiPartFeature.class);
		
		packages(true, "rise.api");
	}
	
	@PostConstruct
	public void initRise() {
		
		RiseLog.debugLog("----------- Welcome to RISE Remote Imaging Support for Emergencies 0.8.1 -----------");

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
	 * Get the User object from the session Id.
	 * The session must exist and not be expired, and the password of the user must not be expired.
	 * @param sSessionId Session token
	 * @return the User or null if the session is not valid
	 */
	public static User getUserFromSession(String sSessionId) {
		return getUserFromSession(sSessionId, true);
	}
	
	/**
	 * Get the User object from the session Id
	 * @param sSessionId Session token
	 * @param bCheckPasswordExpiry if true, returns null when the password of the user is expired
	 * @return the User or null if the session is not valid
	 */
	public static User getUserFromSession(String sSessionId, boolean bCheckPasswordExpiry) {
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		
		User oUser = null;
		String sUserId = "";
		
		try {
			
			SessionRepository oSessionRepository = new SessionRepository();
			Session oUserSession = oSessionRepository.getValidSession(sSessionId);
			
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
			
			if (oUser != null && bCheckPasswordExpiry && PasswordAuthentication.isPasswordExpired(oUser)) {
				RiseLog.warnLog("Rise.getUserFromSession: password expired for user " + oUser.getUserId());
				return null;
			}

		} catch (Exception oE) {
			RiseLog.errorLog("Rise.getUserFromSession: something bad happened: " + oE);
			return null;
		}

		return oUser;
	}	
		
	public static void shutDown() {
		RiseLog.debugLog("RISE is closing, bye bye!");
	}
	
	
	
}
