package rise.lib.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rise.lib.data.MongoRepository;
import rise.lib.utils.Utils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.log.RiseLog;

public class RiseConfig {
	
	public String serverApiAddress = "rise.wasdi.net";
	
	/**
	 * Mongo db configuration
	 */
	public MongoConfig mongoMain;
	
	/**
	 * System name of the wasdi user
	 */
	public String systemUserName = "appwasdi"; //TODO: which system user id?
	
	/**
	 * Id of the system user
	 */
	public Integer systemUserId = 2042;
	
	/**
	 * System name of the wasdi group
	 */
	public String systemGroupName = "appwasdi";
	
	/**
	 * Id of the system group
	 */
	public Integer systemGroupId = 2042;
	
	
	/**
	 * Notifications configuration (mails sent from WASDI to users)
	 */
	public NotificationsConfig notifications;
	
	/**
	 * Available languages
	 */
	public ArrayList<LanguageConfig> localizations = new ArrayList<>();
	
	/**
	 * Security config
	 */
	public SecurityConfig security;
	
	/**
	 * Http Config
	 */
	public HttpConfig httpConfig;
	
	/**
	 * WASDI Config
	 */
	public WasdiConfig wasdiConfig;
	
	/**
	 * Geoserver config
	 */
	public GeoServerConfig geoserver;
	
	/**
	 * Stripe configuration
	 */
	public StripeConfig stripe;
	
	/**
	 * Paths config
	 */
	public PathsConfig paths;
	
	/**
	 * Map of shell exec commands
	 */
	public Map<String, ShellExecItemConfig> shellExecCommands = new HashMap<>();
	
	/**
	 * Controls whether or not Rise should shell exec external components using the local system or using the corresponding docker images.
	 * If False, then we use the dockerized version
	 */
	public boolean shellExecLocally = true;
	
	public DockersConfig dockers = new DockersConfig();
	
	/**
	 * Connection timeout when we call a third party API
	 */
	public int connectionTimeout = 10000;
	
	/**
	 * Set true to activate the logs of the http calls
	 */
	public boolean logHttpCalls = true;
		
	/**
	 * Static Reference to the actual configuration
	 */
	public static RiseConfig Current;
	
	
	
	protected boolean loadLanguageMessages(LanguageConfig oLangConfig) {
		
		if (oLangConfig.messages == null) {
			File oJsonLangFile = new File(oLangConfig.translationsFilePath);
			
			if (oJsonLangFile.exists()) {
				
				Stream<String> oLinesStream = null;
		        try {
		        	
		        	oLinesStream = Files.lines(Paths.get(oLangConfig.translationsFilePath), StandardCharsets.UTF_8);
					String sJson = oLinesStream.collect(Collectors.joining(System.lineSeparator()));
					oLangConfig.messages = MongoRepository.s_oMapper.readValue(sJson,HashMap.class);
					return true;
				} catch (Exception oEx) {
					RiseLog.errorLog("RiseConfig.getMessagesForLanguage: exception ", oEx);
				} finally {
					if (oLinesStream != null) 
						oLinesStream.close();
				}
		        						
			}		
			
			return false;			
		}
		else {
			return true;
		}
	}
	
	/**
	 * Get the Language config for this specific Language
	 * @param oLanguage
	 * @return
	 */
	public LanguageConfig getLanguageConfig(Languages oLanguage) { 
		for (LanguageConfig oLangConfig : localizations) {
			
			if (oLangConfig.languageCode.equals(oLanguage.name())) {
				return oLangConfig;
			}
		}
		
		return null;
	}
	
	/**
	 * Get the dictionary Message Code, Actual Message for the specified Language
	 * @param oLanguage
	 * @return
	 */
	public HashMap<String, String> getMessagesForLanguage(Languages oLanguage) {
		
		for (LanguageConfig oLangConfig : localizations) {
			
			if (oLangConfig.languageCode.equals(oLanguage.name())) {
				
				if (!loadLanguageMessages(oLangConfig)) {
					RiseLog.warnLog("RiseConfig.getMessagesForLanguage: impossible to load language " + oLangConfig.languageCode);
				}
				else {
					return oLangConfig.messages;
				}
			}
		}
		
		// We did not found the languages... Try to back up to default:
		if (oLanguage != Languages.EN) {
			
			LanguageConfig oEnglish = getLanguageConfig(Languages.EN);
			
			if (!loadLanguageMessages(oEnglish)) {
				RiseLog.warnLog("RiseConfig.getMessagesForLanguage: impossible to load language " + oEnglish.languageCode);
			}
			else {
				
				return oEnglish.messages;
			}
		}
		
		RiseLog.warnLog("RiseConfig.getMessagesForLanguage: nothing found, we return an empty dictionary" );
		return new HashMap<>();
	}
	
	
	/**
	 * Read the config from file
	 * @param sConfigFilePath json file path
	 * @return true if ok, false in case of problems
	 */
	public static boolean readConfig(String sConfigFilePath) {
		Stream<String> oLinesStream = null;
		boolean bRes = false;
		
        try {
        	
        	oLinesStream = Files.lines(Paths.get(sConfigFilePath), StandardCharsets.UTF_8);
			String sJson = oLinesStream.collect(Collectors.joining(System.lineSeparator()));
			Current = MongoRepository.s_oMapper.readValue(sJson,RiseConfig.class);
			Current.paths.riseConfigPath = sConfigFilePath;
			bRes = true;
			
		} catch (Exception oEx) {
			RiseLog.errorLog("WasdiConfig.readConfig: exception ", oEx);
		} finally {
			if (oLinesStream != null) 
				oLinesStream.close();
		}
        
        return bRes;
	}	
	
	/**
	 * Safe get a ShellExecItemConfig
	 * @param sCommand Command we are searching for
	 * @return Equivalent ShellExecItemConfig or null in case of any problem
	 */
	public ShellExecItemConfig getShellExecItem(String sCommand) {
		
		// Check if we have the  command
		if (Utils.isNullOrEmpty(sCommand)) {
			RiseLog.warnLog("DockersConfig.getShellExecItem: the command is null or empty");
			return null;
		}
		
		// Check if we have the maps of commands
		if (shellExecCommands == null) {
			RiseLog.warnLog("DockersConfig.getShellExecItem: the map dictionary is null");
			return null;			
		}
		
		try {
			
			// Get just the command, without any path
			File oCommandAsFile = new File(sCommand);
			String sSimplifiedCommand = oCommandAsFile.getName();
			
			// We need to have a command!
			if (Utils.isNullOrEmpty(sSimplifiedCommand)) {
				RiseLog.warnLog("DockersConfig.getShellExecItem: impossible to get the command without paths");
				return null;				
			}
			
			// Is this in the map?
			if (shellExecCommands.containsKey(sSimplifiedCommand)) {
				// Ok return the right ShellExecItemConfig
				return shellExecCommands.get(sSimplifiedCommand);
			}
			else {
				// We do not have it
				RiseLog.warnLog("DockersConfig.getShellExecItem: command not found " + sCommand);
				return null;
			}
			
		}
		catch (Exception oEx) {
			// What happened?
			RiseLog.errorLog("DockersConfig.getShellExecItem: Exception getting the command " + sCommand, oEx);
			return null;
		}
	}
}
