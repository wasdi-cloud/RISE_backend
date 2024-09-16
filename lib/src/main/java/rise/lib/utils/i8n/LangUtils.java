package rise.lib.utils.i8n;

import java.util.HashMap;

import rise.lib.config.RiseConfig;

/**
 * Language Utils: give access to localized strings in the server side
 */
public class LangUtils {

	/**
	 * Get a localized string using default EN language
	 * @param sMessageCode Message code, supposed to be in the rise.lib.util.i8n.StringCodes enum
	 * @return Localized string if available
	 */
	public static String getLocalizedString(String sMessageCode) {
		return getLocalizedString(sMessageCode, Languages.EN.name());
	}
	
	/**
	 * Get a localized string using sLanguage
	 * @param sMessageCode Message code, supposed to be in the rise.lib.util.i8n.StringCodes enum
	 * @param sLanguage Language, should be in the rise.lib.util.i8n.Languages enum
	 * @return
	 */
	public static String getLocalizedString(String sMessageCode, String sLanguage) {
		
		// Get the language
		Languages oLang = Languages.valueOf(sLanguage);
		
		// Default fallback
		if (oLang == null) oLang = Languages.EN;
		
		// Get the messages for this language
		HashMap<String, String> asMessages = RiseConfig.Current.getMessagesForLanguage(oLang);
		
		// Check if it is ok otherwise we return the code itself
		if (asMessages == null) return sMessageCode;
		
		// If the language has this we return it
		if (asMessages.containsKey(sMessageCode)) return asMessages.get(sMessageCode);
		// Return the code
		else return sMessageCode;
	}	
}
