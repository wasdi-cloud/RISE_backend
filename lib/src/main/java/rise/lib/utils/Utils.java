package rise.lib.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// email, IP addresses (v4 and v6), domains and URL validators:
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

import rise.lib.utils.log.RiseLog;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {


	public static int s_iSessionValidityMinutes = 24 * 60;
	private static SecureRandom s_oUtilsRandom = new SecureRandom();

	/**
	 * Private constructor
	 */
	private Utils() {
		throw new IllegalStateException("Utils.Utils: this is just a utility class, please do not instantiate it");
	}
	
	/**
	 * Checks if a string is null or empty
	 * @param sString String to check
	 * @return true if it is null or empty. False if it is a valud string
	 */
	public static boolean isNullOrEmpty(String sString) {
		return sString == null || sString.isEmpty();
	}
	
	/**
	 * Checks if a Double is null or empty
	 * @param oDoube
	 * @return
	 */
	public static boolean isNullOrEmpty(Double oDouble) {
		return oDouble == null || oDouble.longValue() == 0;
	}

	/**
	 * Get a random name capped to a specific length
	 * adapted from:
	 * 4. Generate Random Alphanumeric String With Java 8
	 * https://www.baeldung.com/java-random-string 
	 * @param iLen
	 * @return
	 */
	public static String getCappedRandomName(int iLen) {
		if(iLen < 0) {
			iLen = - iLen;
		}
		
		int iLeftLimit = 48; // numeral '0'
	    int iRightLimit = 122; // letter 'z'
	 
	    return s_oUtilsRandom.ints(iLeftLimit, iRightLimit + 1)
    		//filter method above to leave out Unicode characters between 65 and 90
	    	//to avoid out of range characters.
    		.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
    		.limit(iLen)
    		.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
    		.toString();
	}
	
	/**
	 * Get a random name (ie UUID string)
	 * @return Random name
	 */
	public static String getRandomName() {
		return UUID.randomUUID().toString();
	}
	

	

	/**
	 * Format in a human readable way a file dimension in bytes
	 * @param lBytes
	 * @return
	 */
	public static String getFormatFileDimension(long lBytes) {
		int iUnit = 1024;
		if (lBytes < iUnit)
			return lBytes + " B";
		int iExp = (int) (Math.log(lBytes) / Math.log(iUnit));
		String sPrefix = ("KMGTPE").charAt(iExp - 1) + "";
		return String.format("%.1f %sB", lBytes / Math.pow(iUnit, iExp), sPrefix);
	}
	
	private static char randomChar() {
		return (char) (s_oUtilsRandom.nextInt(26) + 'a');
	}

	public static String generateRandomPassword() {
		String sPassword = UUID.randomUUID().toString();
		sPassword = sPassword.replace('-', randomChar());
		return sPassword;
	}


	public static boolean isServerNamePlausible(String sServer) {
		if (isNullOrEmpty(sServer)) {
			return false;
		}
		// Ok, let's inspect the server...
		boolean bRes = false;
		bRes = InetAddressValidator.getInstance().isValid(sServer);
		if (!bRes) {
			// then maybe it's a domain
			bRes = DomainValidator.getInstance().isValid(sServer);
		}
		if (!bRes) {
			// then maybe it's an URL
			bRes = UrlValidator.getInstance().isValid(sServer);
		}
		if (!bRes) {
			// then maybe it's localhost
			bRes = sServer.equals("localhost");
		}
		return bRes;
	}

	public static Boolean isPortNumberPlausible(Integer iPort) {
		if (null == iPort) {
			return false;
		}
		if (0 <= iPort && iPort <= 65535) {
			return true;
		}
		return false;
	}

	public static String[] convertPolygonToArray(String sArea) {
		String[] asAreaPoints = new String[0];
		if (sArea.isEmpty()) {
			return asAreaPoints;
		}

		try {
			String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
			asAreaPoints = sCleanedArea.split(",");
		} catch (Exception oE) {
			RiseLog.debugLog("Utils.convertPolygonToArray( " + sArea + "  ): could not extract area points due to " + oE); 
		}
		return asAreaPoints;
	}

	public static boolean doesThisStringMeansTrue(String sString) {
		// default value is arbitrary!
		return (
				isNullOrEmpty(sString) ||
				sString.equalsIgnoreCase("true") ||
				sString.equalsIgnoreCase("t") ||
				sString.equalsIgnoreCase("1") ||
				sString.equalsIgnoreCase("yes") ||
				sString.equalsIgnoreCase("y")
		);
	}
	
	
	///////// units conversion
	private static String[] sUnits = {"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"}; //...yeah, ready for the decades to come :-O
	
	public static String getNormalizedSize(double dSize, String sInputUnit) {
		for(int i = 0; i < sUnits.length; ++i) {
			if((sUnits[i]).equals(sInputUnit)) {
				return getNormalizedSize(dSize, i);
			}
		}
		RiseLog.log("WARNING", "Utils.getNormalizedSize( " + dSize + ", " + sInputUnit + " ): could not find requested unit");
		return "";
	}
	
	public static String getNormalizedSize(Double dSize) {
		return getNormalizedSize(dSize, 0);
	}
	
	public static String getNormalizedSize(Double dSize, int iStartingIndex) {
		String sChosenUnit = sUnits[iStartingIndex];
		String sSize = Long.toString(Math.round(dSize)) + " " + sChosenUnit;
		 
		int iUnitIndex = Math.max(0, iStartingIndex);
		int iLim = sUnits.length -1;
		while(iUnitIndex < iLim && dSize >= 900.0) {
			dSize = dSize / 1024.0;
			iUnitIndex++;

			//now, round it to two decimal digits
			dSize = Math.round(dSize*100.0)/100.0; 
			sChosenUnit = sUnits[iUnitIndex];
			sSize = String.valueOf(dSize) + " " + sChosenUnit;
		}
		return sSize;
	}

	///// end units conversion
	
	/**
	 * Get Random Number in Range
	 * @param iMin
	 * @param iMax
	 * @return
	 */
	public static int getRandomNumber(int iMin, int iMax) {
		return iMin + new SecureRandom().nextInt(iMax - iMin);
	}
	
	/**
	 * Generates and OPT Password composed buy six numbers
	 * @return
	 */
	public static String getOTPPassword() {
		int iNumber1 = getRandomNumber(0, 999);
		int iNumber2 = getRandomNumber(0, 999);
		
		String sOTPPassword= "" + iNumber1 + "" + iNumber2;
		
		return sOTPPassword;
	}

	/**
	 * Get a clone of the workspace name.
	 * If the name ends with an ordinal (i.e. 1) it is increased (i.e. 2).
	 * Otherwise, it appends the (1) termination
	 * @param sOriginalName the original name of the workspace
	 * @return the new name of the workspace
	 */
	public static String cloneName(String sOriginalName) {

		if (sOriginalName == null || sOriginalName.isEmpty()) {
			return "Untitled Workspace";
		}

		List<String> asTokens = Arrays.asList(sOriginalName.split("[\\(\\)]"));

		String sNewName;

		if (asTokens.size() == 1) {
			sNewName = sOriginalName + "(1)";
		} else {
			String sLastToken = asTokens.get(asTokens.size() - 1);

			try {
				int iOrdinal = Integer.parseInt(sLastToken);
				int iIncrementedOrdinal = iOrdinal + 1;
				int iIndex = sOriginalName.lastIndexOf(sLastToken);
				sNewName = sOriginalName.substring(0, iIndex) + iIncrementedOrdinal + ")";
			} catch (NumberFormatException e) {
				sNewName = sOriginalName + "(1)";
			}
		}

		return sNewName;
	}
		
    /**
     * Function to remove duplicates from an ArrayList 
     * @param <T> Type
     * @param aoOriginalList
     * @return
     */
    public static <T> ArrayList<T> removeDuplicates(ArrayList<T> aoOriginalList) 
    { 
  
        // Create a new LinkedHashSet 
        Set<T> oUniqueSet = new LinkedHashSet<>(); 
  
        // Add the elements to set 
        oUniqueSet.addAll(aoOriginalList); 
  
        // Clear the list 
        aoOriginalList.clear(); 
  
        // add the elements of set 
        // with no duplicates to the list 
        aoOriginalList.addAll(oUniqueSet); 
  
        // return the list 
        return aoOriginalList; 
    }	

}
