package rise.lib.utils;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import rise.lib.utils.log.RiseLog;


public class JsonUtils {
	
	public static ObjectMapper s_oMapper = new ObjectMapper();

	private JsonUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}
	
	/**
	 * Convert a Map<String, Object> to a string JSON representation
	 * @param aoJSONMap Map to render in Json
	 * @return String with the JSON
	 */
	public static String stringify(Map<String, Object> aoJSONMap) {
		try {
			return s_oMapper.writeValueAsString(aoJSONMap);
		} catch (JsonProcessingException oE) {
			RiseLog.errorLog("JsonUtils.stringify: could not stringify the object due to " + oE + ".");
		}

		return "";
	}
	
	/**
	 * Obtains the string representation of an object
	 * @param object Object to render as JSON
	 * @return String with the JSON
	 */
	public static String stringify(Object object) {
		try {
			return s_oMapper.writeValueAsString(object);
		} catch (JsonProcessingException oE) {
			RiseLog.errorLog("JsonUtils.stringify: could not stringify the object due to " + oE + ".");
		}

		return "";
	}

}
