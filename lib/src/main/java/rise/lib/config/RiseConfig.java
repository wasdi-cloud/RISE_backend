package rise.lib.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rise.lib.data.MongoRepository;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.log.RiseLog;

public class RiseConfig {
	
	public String serverApiAddress = "rise.wasdi.net";
	
	/**
	 * Mongo db configuration
	 */
	public MongoConfig mongoMain;
	
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
			//Current.paths.wasdiConfigFilePath = sConfigFilePath;
			bRes = true;
			
		} catch (Exception oEx) {
			RiseLog.errorLog("WasdiConfig.readConfig: exception ", oEx);
		} finally {
			if (oLinesStream != null) 
				oLinesStream.close();
		}
        
        return bRes;
	}	
}
