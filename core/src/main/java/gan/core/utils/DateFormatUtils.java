package gan.core.utils;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class DateFormatUtils {
	
	private static final ThreadLocal<HashMap<String,  SoftReference<SimpleDateFormat>>> tl = new ThreadLocal<HashMap<String,SoftReference<SimpleDateFormat>>>(){
		
		@Override
		protected HashMap<String,SoftReference<SimpleDateFormat>> initialValue() {
			return new HashMap<String, SoftReference<SimpleDateFormat>>();
		}
	};
	    
	public static SimpleDateFormat getYMdHm(){
		return getDateFormat("y年M月d日 HH:mm");
	}
	
	public static SimpleDateFormat getMdHm(){
		return getDateFormat("MM月dd日 HH:mm");
	}
	
	public static SimpleDateFormat getHms(){
		return getDateFormat("00:mm:ss");
	}
	
	public static SimpleDateFormat getMs(){
		return getDateFormat("mm:ss");
	}
	
	public static SimpleDateFormat getYM(){
		return getDateFormat("y.MM");
	}
	
	public static SimpleDateFormat getYMd(){
		return getDateFormat("y年M月d日");
	}
	
	public static SimpleDateFormat getMd(){
		return getDateFormat("M月d日");
	}
	
	public static SimpleDateFormat getBarsYMdHm(){
		return getDateFormat("y-M-d HH:mm");
	}
	
	public static SimpleDateFormat getBarsMdHm(){
		return getDateFormat("M-d HH:mm");
	}
	
	public static SimpleDateFormat getBarsMd(){
		return getDateFormat("M-d");
	}
	
	public static SimpleDateFormat getBarsYMd(){
		return getDateFormat("y-M-d");
	}
	
	public static SimpleDateFormat getBarsYMdHms(){
		return getDateFormat("y-M-d HH:mm:ss");
	}
	
	public static SimpleDateFormat getBarsMdHms(){
		return getDateFormat("M-d HH:mm:ss");
	}
	
	public static SimpleDateFormat getBarsYM(){
		return getDateFormat("y-M");
	}
	
	public static SimpleDateFormat getDirectYMd(){
		return getDateFormat("yyyyMMdd");
	}
	
	public static SimpleDateFormat getDirectYM(){
		return getDateFormat("yMM");
	}
	
	public static SimpleDateFormat getHm(){
		return getDateFormat("HH:mm");
	}
	
	public static SimpleDateFormat getE(){
		return getDateFormat("E");
	}
	
	public static SimpleDateFormat getDotYMd(){
		return getDateFormat("y.M.d");
	}
	
	public static SimpleDateFormat getDotMd(){
		return getDateFormat("M.d");
	}
	
	public static SimpleDateFormat getSplashYMd(){
		return getDateFormat("y/M/d");
	}
	
	public static SimpleDateFormat getSplashYMdHm(){
		return getDateFormat("y/M/d HH:mm");
	}
	
	public static SimpleDateFormat getDateFormat(String pattern){
		HashMap<String, SoftReference<SimpleDateFormat>> map = tl.get();
		
		SoftReference<SimpleDateFormat> sf = map.get(pattern);
		SimpleDateFormat df = sf == null ? null : sf.get();
		if(df == null){
			df = new SimpleDateFormat(pattern);
			map.put(pattern, new SoftReference<SimpleDateFormat>(df));
		}
		return df;
	}
	
	public static String formatMd(long time) {
		time = time * 1000;
		try {
			if (DateUtils.isInCurrentYear(time)) {
				return getMd().format(new Date(time));
			} else {
				return getYMd().format(new Date(time));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String formatMdHm(long time) {
		time = time * 1000;
		try {
			if (DateUtils.isInCurrentYear(time)) {
				return getMdHm().format(new Date(time));
			} else {
				return getYMdHm().format(new Date(time));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String formatCharacterYMdHm(long time){
		time = time * 1000;
		try {
			if (DateUtils.isInCurrentYear(time)) {
				return getMdHm().format(new Date(time));
			} else {
				return getYMdHm().format(new Date(time));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String formatBarsYMdHm(long time){
		time = time * 1000;
		try {
			if (DateUtils.isInCurrentYear(time)) {
				return getBarsMdHm().format(new Date(time));
			} else {
				return getBarsYMdHm().format(new Date(time));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String formatBarsYMdHms(long time){
		time = time * 1000;
		try {
			if (DateUtils.isInCurrentYear(time)) {
				return getBarsMdHms().format(new Date(time));
			} else {
				return getBarsYMdHms().format(new Date(time));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String formatBarsYMd(long time){
		time = time * 1000;
		try {
			if (DateUtils.isInCurrentYear(time)) {
				return getBarsMd().format(new Date(time));
			} else {
				return getBarsYMd().format(new Date(time));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String format(long time, SimpleDateFormat df) {
		time = time * 1000;
		try {
			return df.format(new Date(time));
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static long	parseTime(String time,SimpleDateFormat df){
		try{
			return df.parse(time).getTime();
		}catch(Exception e){
		}
		return 0;
	}
	
	public static long parseTime(String time,String pattern){
		try{
			return getDateFormat(pattern).parse(time).getTime();
		}catch(Exception e){
		}
		return 0;
	}
}
