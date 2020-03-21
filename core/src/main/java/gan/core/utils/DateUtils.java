package gan.core.utils;

import gan.core.system.server.SystemServer;

import java.util.Calendar;

public class DateUtils {
	
	public static final ThreadLocal<Calendar> ThreadLocalCalendar = new ThreadLocal<Calendar>() {
	    @Override 
	    protected Calendar initialValue() {
	        return Calendar.getInstance();
	    }
	};
	
	public static final ThreadLocal<Calendar> ThreadLocalCalendar2 = new ThreadLocal<Calendar>(){
		@Override
		protected Calendar initialValue() {
			return Calendar.getInstance();
		}
	};
	
	public static long getTimeNextDay(long lTimeMillis){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(lTimeMillis);
		cal.add(Calendar.DAY_OF_YEAR, 1);
		return cal.getTimeInMillis();
	}
	
	public static long getTimePrevDay(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.add(Calendar.DAY_OF_YEAR, -1);
		return cal.getTimeInMillis();
	}
	
	public static long getTimePrevMonth(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.add(Calendar.MONTH, -1);
		return cal.getTimeInMillis();
	}
	
	public static long getTimeNextMonth(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.add(Calendar.MONTH, 1);
		return cal.getTimeInMillis();
	}
	
	public static long getPrevWeek(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.add(Calendar.WEEK_OF_YEAR, -1);
		return cal.getTimeInMillis();
	}
	
	public static long getNextWeek(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.add(Calendar.WEEK_OF_YEAR, 1);
		return cal.getTimeInMillis();
	}
	
	public static boolean isToday(long lTime){
		return isDateDayEqual(lTime, System.currentTimeMillis());
	}
	
	public static boolean isTomorrow(long lTime){
		return isDateDayEqual(lTime, getTimeNextDay(System.currentTimeMillis()));
	}
	
	public static boolean isYestoday(long time){
		return isDateDayEqual(time, getTimePrevDay(System.currentTimeMillis()));
	}
	
	public static boolean isDateDayEqual(long lTime1,long lTime2){
		Calendar cal1 = ThreadLocalCalendar.get();
		cal1.setTimeInMillis(lTime1);
		final int year1 = cal1.get(Calendar.YEAR);
		final int day1 = cal1.get(Calendar.DAY_OF_YEAR);
		cal1.setTimeInMillis(lTime2);
		
		return year1 == cal1.get(Calendar.YEAR) &&
				day1 == cal1.get(Calendar.DAY_OF_YEAR);
	}
	
	public static boolean isInSameWeek(long time1,long time2){
		Calendar calA = ThreadLocalCalendar.get();
		calA.setTimeInMillis(time1);
		if(calA.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
			calA.add(Calendar.WEEK_OF_YEAR, -1);
		}
		final int year1 = calA.get(Calendar.YEAR);
		final int week1 = calA.get(Calendar.WEEK_OF_YEAR);
		calA.setTimeInMillis(time2);
		if(calA.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
			calA.add(Calendar.WEEK_OF_YEAR, -1);
		}
		if(year1 == calA.get(Calendar.YEAR) &&
				week1 == calA.get(Calendar.WEEK_OF_YEAR)){
			return true;
		}
		return false;
	}
	
	public static boolean isInLastWeek(long lTime){
		Calendar calToday = ThreadLocalCalendar.get();
		Calendar calUnknown = ThreadLocalCalendar2.get();
		calToday.setTimeInMillis(System.currentTimeMillis());
		calToday.set(Calendar.HOUR_OF_DAY, 0);
		calToday.set(Calendar.MINUTE, 0);
		calToday.set(Calendar.SECOND, 0);
		calToday.set(Calendar.MILLISECOND, 0);
		calUnknown.setTimeInMillis(lTime);
		int nDayOfWeek = calToday.get(Calendar.DAY_OF_WEEK);
		if(nDayOfWeek == Calendar.SUNDAY){
			nDayOfWeek = Calendar.SATURDAY + 1;
		}
		nDayOfWeek -= 1;
		calToday.add(Calendar.DAY_OF_YEAR, -(nDayOfWeek - 1));
		if(calUnknown.after(calToday)){
			return false;
		}
		calToday.add(Calendar.DAY_OF_YEAR, -7);
		if(calUnknown.before(calToday)){
			return false;
		}
		return true;
	}
	
	public static boolean isInCurrentWeek(long lTime) {
		Calendar calToday = ThreadLocalCalendar.get();
		Calendar calUnknown = ThreadLocalCalendar2.get();
		calToday.setTimeInMillis(SystemServer.currentTimeMillis());
		calToday.set(Calendar.HOUR_OF_DAY, 0);
		calToday.set(Calendar.MINUTE, 0);
		calToday.set(Calendar.SECOND, 0);
		calToday.set(Calendar.MILLISECOND, 0);
		calUnknown.setTimeInMillis(lTime);
		int nDayOfWeek = calToday.get(Calendar.DAY_OF_WEEK);
		if (nDayOfWeek == Calendar.SUNDAY) {
			nDayOfWeek = Calendar.SATURDAY + 1;
		}
		nDayOfWeek -= 1;
		calToday.add(Calendar.DAY_OF_YEAR, -(nDayOfWeek - 1));
		if (calUnknown.before(calToday)) {
			return false;
		}
		calToday.add(Calendar.DAY_OF_YEAR, 7);
		if (calUnknown.after(calToday)) {
			return false;
		}
		return true;
	}
	
	public static long getWeekFirstDay(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		int nDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (nDayOfWeek == Calendar.SUNDAY) {
			nDayOfWeek = Calendar.SATURDAY + 1;
		}
		nDayOfWeek -= 1;
		cal.add(Calendar.DAY_OF_YEAR, -(nDayOfWeek - 1));
		return cal.getTimeInMillis();
	}
	
	public static long getWeekLastDay(long time){
		long first = getWeekFirstDay(time);
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(first);
		cal.add(Calendar.DAY_OF_YEAR, 7);
		cal.add(Calendar.MILLISECOND, -1);
		return cal.getTimeInMillis();
	}
	
	public static boolean isInNextWeek(long lTime) {
		Calendar calToday = ThreadLocalCalendar.get();
		Calendar calUnknown = ThreadLocalCalendar2.get();
		calToday.setTimeInMillis(SystemServer.currentTimeMillis());
		calUnknown.setTimeInMillis(lTime);
		int nDayOfWeek = calToday.get(Calendar.DAY_OF_WEEK);
		if (nDayOfWeek == Calendar.SUNDAY) {
			nDayOfWeek = Calendar.SATURDAY + 1;
		}
		nDayOfWeek -= 1;
		calToday.add(Calendar.DAY_OF_YEAR, 8 - nDayOfWeek);
		if (calUnknown.before(calToday)) {
			return false;
		}
		calToday.add(Calendar.DAY_OF_YEAR, 7);
		if (calUnknown.after(calToday)) {
			return false;
		}
		return true;
	}
	
	public static boolean isBeyondNextWeek(long lTime) {
		Calendar calToday = ThreadLocalCalendar.get();
		Calendar calUnknown = ThreadLocalCalendar2.get();
		calToday.setTimeInMillis(SystemServer.currentTimeMillis());
		calUnknown.setTimeInMillis(lTime);
		int nDayOfWeek = calToday.get(Calendar.DAY_OF_WEEK);
		if (nDayOfWeek == Calendar.SUNDAY) {
			nDayOfWeek = Calendar.SATURDAY + 1;
		}
		nDayOfWeek -= 1;
		calToday.add(Calendar.DAY_OF_YEAR, 8 - nDayOfWeek);
		if (calUnknown.before(calToday)) {
			return false;
		}
		calToday.add(Calendar.DAY_OF_YEAR, 7);
		if (calUnknown.after(calToday)) {
			return true;
		}
		return false;
	}
	
	public static boolean isInCurrentMonth(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(SystemServer.currentTimeMillis());
		Calendar calTime = ThreadLocalCalendar2.get();
		calTime.setTimeInMillis(time);
		if(cal.get(Calendar.YEAR) == calTime.get(Calendar.YEAR) &&
				cal.get(Calendar.MONTH) == calTime.get(Calendar.MONTH)){
			return true;
		}
		return false;
	}
	
	public static long getMonthFirstDay(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		cal.add(Calendar.DAY_OF_YEAR, 1 - dayOfMonth);
		return cal.getTimeInMillis();
	}
	
	public static long getMonthLastDay(long time){
		long first = getMonthFirstDay(time);
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(first);
		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.MILLISECOND, -1);
		return cal.getTimeInMillis();
	}
	
	public static long getQuarterFirstDay(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		int startMonth = getQuarterMonth(cal.get(Calendar.MONTH), true);
		cal.set(Calendar.MONTH, startMonth);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		return cal.getTimeInMillis();
	}
	
	public static long getQuarterLastDay(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		int endMonth = getQuarterMonth(cal.get(Calendar.MONTH), false);
		cal.set(Calendar.MONTH, endMonth + 1);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.MILLISECOND, -1);
		return cal.getTimeInMillis();
	}
	
	public static int getQuarterMonth(int month, boolean isQuarterStart) {  
        int months[] = { 0, 3, 6, 9 };
        if (!isQuarterStart) {  
            months = new int[] { 2, 5, 8, 11 };  
        }  
        if (month >= 0 && month <= 2)  
            return months[0];  
        else if (month >= 3 && month <= 5)  
            return months[1];  
        else if (month >= 6 && month <= 8)  
            return months[2];  
        else  
            return months[3];  
    }  
	
	public static boolean isInCurrentYear(long lTime) {
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(SystemServer.currentTimeMillis());
		int year = cal.get(Calendar.YEAR);
		cal.setTimeInMillis(lTime);
		
		return year == cal.get(Calendar.YEAR);
	}
	
	public static boolean isInSameYear(long time1,long time2){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time1);
		int year1 = cal.get(Calendar.YEAR);
		cal.setTimeInMillis(time2);
		return year1 == cal.get(Calendar.YEAR);
	}
	
	public static boolean isInSameMonth(long time1,long time2){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time1);
		Calendar cal2 = ThreadLocalCalendar2.get();
		cal2.setTimeInMillis(time2);
		return (cal.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)) &&
				(cal.get(Calendar.MONTH) == cal2.get(Calendar.MONTH));
	}

	public static long getDayZeroClock(long time){
		Calendar cal = ThreadLocalCalendar.get();
		cal.setTimeInMillis(time);

		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);

		return cal.getTimeInMillis();
	}

	public static long getDayZeroClockSecond(long millseconds) {
		if (String.valueOf(millseconds).length() > 10) {
			Calendar cal = DateUtils.ThreadLocalCalendar.get();
			cal.setTimeInMillis(millseconds);
			setZeroClock(cal);
			return cal.getTimeInMillis() / 1000;
		} else {
			Calendar cal = DateUtils.ThreadLocalCalendar.get();
			cal.setTimeInMillis(millseconds * 1000);
			setZeroClock(cal);
			return cal.getTimeInMillis() / 1000;
		}
	}

	public static long getDayZeroClockMillis(long millseconds) {
		Calendar cal = DateUtils.ThreadLocalCalendar.get();
		cal.setTimeInMillis(millseconds);
		setZeroClock(cal);
		return cal.getTimeInMillis();
	}

	public static void setZeroClock(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}

	public static long getDayLastSecond(long milliseconds) {
		if (milliseconds == 0) {
			return 0;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(milliseconds);
		setDayLast(cal);
		return cal.getTimeInMillis() / 1000;
	}

	public static long getDayLastMillis(long milliseconds) {
		if (milliseconds == 0) {
			return 0;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(milliseconds);
		setDayLast(cal);
		return cal.getTimeInMillis();
	}

	public static void setDayLast(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
	}
}
