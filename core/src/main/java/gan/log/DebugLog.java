package gan.log;

public class DebugLog {

	public final static int WARNING = 1;
	public final static int INFO = 2;
	public final static int DEBUG = 3;
	public final static int NOTHING = Integer.MIN_VALUE;
	public final static int ALL = Integer.MAX_VALUE;
	public static int Level = INFO;
	private static FileLogger InfoLogger = FileLogger.getInfoLogger();

	public static void setLevel(int level) {
		Level = level;
	}

	public static void info(String format,Object... arg){
		if(Level<=NOTHING){
			return;
		}
		if(Level>=INFO){
			InfoLogger.log(format,arg);
		}
	}

	public static void debug(String format,Object... arg) {
		if(Level<=NOTHING){
			return;
		}
		if(Level>=DEBUG) {
			InfoLogger.log(format,arg);
		}
	}

	public static void warn(String format,Object... arg){
		if(Level<=NOTHING){
			return;
		}
		if(Level>=WARNING){
			InfoLogger.log(format,arg);
		}
	}

}
