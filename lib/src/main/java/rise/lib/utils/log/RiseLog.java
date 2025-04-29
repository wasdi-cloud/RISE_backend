package rise.lib.utils.log;

import rise.lib.utils.Utils;

public class RiseLog {
	/**
	 * Reference to the logger wrapper to use
	 */
	protected static LoggerWrapper s_oLoggerWrapper = null;
	
	/**
	 * Prefix to the log strings
	 */
	protected static String s_sPrefix = "";
	
	/**
	 * Set the active logger wrapper
	 * @param oLoggerWrapper
	 */
	public static void setLoggerWrapper(LoggerWrapper oLoggerWrapper) {
		s_oLoggerWrapper = oLoggerWrapper;
	}	
	
	/**
	 * Debug Log
	 * 
	 * @param sMessage
	 */
	public static void debugLog(String sMessage) {
		log(RiseLogLevels.DEBUG, sMessage);
	}
	
	/**
	 * Info log
	 * @param sMessage
	 */
	public static void infoLog(String sMessage) {
		log(RiseLogLevels.INFO, sMessage);
	}
	
	/**
	 * Warning log
	 * @param sMessage
	 */
	public static void warnLog(String sMessage) {
		log(RiseLogLevels.WARNING, sMessage);
	}
	
	/**
	 * Error log
	 * @param sMessage
	 */
	public static void errorLog(String sMessage) {
		log(RiseLogLevels.ERROR, sMessage);
	}
	
	/**
	 * Error log
	 * @param sMessage
	 */
	public static void errorLog(String sMessage, Exception oEx) {
		String sException = "";
		
		if (oEx != null)  {
			sException = " - " + oEx.toString();
		}
		log(RiseLogLevels.ERROR, sMessage + sException);
	}	
	
	/**
	 * Log
	 * @param oLevel Log Level
	 * @param sMessage Log Message
	 */
	public static void log(RiseLogLevels oLevel, String sMessage) {
		log(oLevel.name(), sMessage);
	}
	
	/**
	 * Log
	 * @param sLevel Log Level
	 * @param sMessage Log Message
	 */
	public static void log(String sLevel, String sMessage) {
		String sPrefix = "";
		if(!Utils.isNullOrEmpty(sLevel)) {
			sPrefix = "[" + sLevel + "] ";
		}
		
		//LocalDateTime oNow = LocalDateTime.now();
		
		if (s_oLoggerWrapper != null) {
			
			s_oLoggerWrapper.setPrefix(s_sPrefix);
			
			if (sLevel.equals(RiseLogLevels.DEBUG.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.debug(sMessage);
				}
			}
			else if (sLevel.equals(RiseLogLevels.INFO.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.info(sMessage);
				}
			}
			else if (sLevel.equals(RiseLogLevels.WARNING.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.warn(sMessage);
				}
			}
			else if (sLevel.equals(RiseLogLevels.ERROR.name())) {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.error(sMessage);
				}
			}
			else {
				synchronized (s_oLoggerWrapper) {
					s_oLoggerWrapper.info(sMessage);
				}
			}
		}
		else {
			String sFinalLine = sPrefix;
			
//			if (WasdiConfig.Current!=null) {
//				if (WasdiConfig.Current.addDateTimeToLogs) {
//					sFinalLine += "" + oNow + " "; 
//				}				
//			}
			
			sFinalLine+= s_sPrefix + ": " + sMessage;
			System.out.println(sFinalLine);
		}
	}

	public static String getPrefix() {
		return s_sPrefix;
	}

	public static void setPrefix(String sPrefix) {
		s_sPrefix = sPrefix;
	}
}
