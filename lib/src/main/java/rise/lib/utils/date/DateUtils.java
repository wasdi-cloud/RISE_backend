package rise.lib.utils.date;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

public class DateUtils {
	public static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_yyyyMMdd = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));
	public static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_yyyyMMddTZ = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

	/**
	 * Convert a Double in a date assuming it contains a valid timestamp
	 * @param oDouble Input timestamp
	 * @return Corresponding date
	 */
	public static Date getDate(Double oDouble) {
		if (oDouble == null) {
			return null;
		}

		double dDate = oDouble;
		long lLong = (long) dDate;
		return new Date(lLong);
	}
	
	/**
	 * Gets now as double
	 * @return
	 */
	public static Double getNowAsDouble() {
		return getDateAsDouble(new Date());
	}

	/**
	 * Get a date as a Double timestamp
	 * @param oDate
	 * @return
	 */
	public static Double getDateAsDouble(Date oDate) {
		if (oDate == null) {
			return null;
		}

		return (double) (oDate.getTime());
	}

	public static Date getDate(Long oLong) {
		return new Date(oLong);
	}


	/**
	 * Format the date using the yyyyMMdd date format.
	 * @param oDate the date to be formatted
	 * @return the string containing the formatted date
	 */
	public static String formatToYyyyMMdd(Date oDate) {
		return new SimpleDateFormat("yyyyMMdd").format(oDate);
	}
	
	public static String getFormatDate(Date oDate) {

		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(oDate);
	}

	public static String getFormatDate(Double oDouble) {

		if (oDouble == null) {
			return null;
		}

		return getFormatDate(new Date(oDouble.longValue()));
	}

	public static String formatToYyyyDashMMDashdd(Date oDate) {
		return new SimpleDateFormat("yyyy-MM-dd").format(oDate);
	}

	public static Date getWasdiDate(String sWasdiDate) {

		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sWasdiDate);
		} catch (Exception oE) {
			RiseLog.errorLog("Utils.getWasdiDate( " + sWasdiDate + "  ): could not be parsed due to " + oE);
			return null;
		}
	}

	public static Date getYyyyMMddDate(String sDate) {

		try {
			return SIMPLE_DATE_FORMAT_yyyyMMdd.get().parse(sDate);
		} catch (Exception oE) {
			RiseLog.errorLog("Utils.getYyyyMMddDate( " + sDate + "  ): could not be parsed due to " + oE);
			return null;
		}
	}

	public static Date getYyyyMMddTZDate(String sDate) {
		if (Utils.isNullOrEmpty(sDate)) {
			return null;
		}

		try {
			return SIMPLE_DATE_FORMAT_yyyyMMddTZ.get().parse(sDate);
		} catch (Exception oE) {
			RiseLog.errorLog("Utils.getYyyyMMddTZDate( " + sDate + "  ): could not be parsed due to " + oE);
			return null;
		}
	}

	/**
	 * Parse the date into a Double fit for MongoDb.
	 * @param sWasdiDate the date as a string in the yyyy-MM-dd HH:mm:ss format
	 * @return the time in millis in the form of a Double
	 */
	public static Double getWasdiDateAsDouble(String sWasdiDate) {
		if (sWasdiDate == null) {
			return null;
		}

		Date oDate = getWasdiDate(sWasdiDate);

		if (oDate == null) {
			return null;
		}

		long lTimeInMillis = oDate.getTime();

		return Double.valueOf(lTimeInMillis);
	}
	
	
	/**
	 * Gets the local time offset from UTC
	 * @return
	 */
	public static String getLocalDateOffsetFromUTCForJS() {
		TimeZone oTimeZone = TimeZone.getDefault();
		GregorianCalendar oCalendar = new GregorianCalendar(oTimeZone);
		int iOffsetInMillis = oTimeZone.getOffset(oCalendar.getTimeInMillis());
		
		if (iOffsetInMillis == 0) return "Z";
		
		String sOffset = String.format("%02d:%02d", Math.abs(iOffsetInMillis / 3600000), Math.abs((iOffsetInMillis / 60000) % 60));
		sOffset = (iOffsetInMillis >= 0 ? "+" : "-") + sOffset;
		return sOffset;
	}

	public static String getDateWithLocalDateOffsetFromUTCForJS(String sDate) {
		if (sDate == null || sDate.isEmpty()) {
			return "";
		}

		return sDate +  " " + getLocalDateOffsetFromUTCForJS();
	}
	
	/**
	 * Split the time range represented by the two input Date objects in monthly intervals.
	 * Each Date array represents an interval and is made of two elements. Element at index 0 represent the start Date of the interval,
	 * while the element at index 1 represents the end Date of the interval. 
	 * E.g. if the input parameters represent an interval from 22/05/2023 to 10/07/203, then the monthly intervals will be: 
	 * [22/05/2023-31/05/2023], [01/06/2023-30/06/2023], [01/07/2023-10/07/2023]
	 * @param oStartDate the start date of the time range
	 * @param oEndDate the end date of the time range
	 * @param iOffset offset for the pagination
	 * @param iLimit maximum number of results per page
	 * @return a list of monthly intervals included in the time range, represented by 2-dimensional Date arrays.
	 */
	public static List<Date[]> splitTimeRangeInMonthlyIntervals(Date oStartDate, Date oEndDate, int iOffset, int iLimit) {
		List<Date[]> aaoIntervals = new LinkedList<>();
		Calendar oStartCalendar = Calendar.getInstance();
		oStartCalendar.setTime(oStartDate);
		Calendar oEndCalendar = Calendar.getInstance();
		oEndCalendar.setTime(oEndDate);
		
		int iCurrentInterval = 0;
		while (oStartCalendar.before(oEndCalendar)) {
			Date[] aoCurrentInterval = getMonthIntervalFromDate(oStartCalendar, oEndCalendar);
			if (aoCurrentInterval.length == 0) {
				break;
			}
			
			if (iCurrentInterval >= iOffset && iCurrentInterval < iOffset + iLimit) {
				aaoIntervals.add(aoCurrentInterval);
			}
			
			oStartCalendar.setTime(aoCurrentInterval[1]);
			oStartCalendar.add(Calendar.MILLISECOND, 1);
			
			iCurrentInterval++;
		}
		return aaoIntervals;
	}
	
	
	private static Date[] getMonthIntervalFromDate(Calendar oStartCalendar, Calendar oEndCalendar) {
		Calendar oStartCalendarClone = Calendar.getInstance();
		oStartCalendarClone.setTime(oStartCalendar.getTime());
		int iStartMonth = oStartCalendarClone.get(Calendar.MONTH);
		int iEndMonth = oEndCalendar.get(Calendar.MONTH);
		int iStartYear = oStartCalendarClone.get(Calendar.YEAR);
		int iEndYear = oEndCalendar.get(Calendar.YEAR);
		
		if (iStartMonth == iEndMonth && iStartYear == iEndYear) {
			Date oStartIntervalDate = oStartCalendarClone.getTime();
			Date oEndIntervalDate = oEndCalendar.getTime();
			Date[] aoInterval = new Date[] {oStartIntervalDate, oEndIntervalDate};
			return aoInterval;
		} else {
			Date oStartIntervalDate = oStartCalendarClone.getTime();
			// jump to the last day of the month		
			oStartCalendarClone.set(Calendar.DAY_OF_MONTH, 1);
			oStartCalendarClone.set(Calendar.MONTH, (iStartMonth + 1) % 12);
			adjustCalendar(oStartCalendarClone);
			oStartCalendarClone.add(Calendar.MILLISECOND, -1);

		
			Date oEndIntervalDate = oStartCalendarClone.getTime();
			Date[] aoIntervals = new Date[] {oStartIntervalDate, oEndIntervalDate};
			return aoIntervals;
		}
	}
	
	private static void adjustCalendar(Calendar oCalendar) {
		if (oCalendar.get(Calendar.MONTH) == 0) {
			oCalendar.add(Calendar.YEAR, 1);
		}

	}
	
	/**
	 * Get the current time in millis as a Double.
	 * @return a Double object
	 */
	public static Double nowInMillis() {
		return (double) new Date().getTime();
	}
	
}
