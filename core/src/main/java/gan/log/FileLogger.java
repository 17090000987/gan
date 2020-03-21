package gan.log;

import gan.core.RecycleObjectPool;
import gan.core.Recycleable;
import gan.core.file.FileHelper;
import gan.core.utils.DateUtils;
import gan.core.utils.TextUtils;
import gan.core.system.SystemUtils;
import gan.core.system.server.SystemServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FileLogger {

	public static FileLogger getDebugLogger(){
		return FileLogger.getInstance("debug").setMaxFileNum(1);
	}

	public static FileLogger getInfoLogger(){
		return FileLogger.getInstance("info")
				.setMaxFileNum(3);
	}

	public static FileLogger getExceptionLogger(){
		return FileLogger.getInstance("exception")
				.setLogcat(true)
				.setMaxFileNum(1);
	}

	public synchronized static FileLogger getInstance(String filePath){
		FileLogger fl = mapNameToLogger.get(filePath);
		if(fl == null){
			fl = new FileLogger(filePath);
			mapNameToLogger.put(filePath, fl);
		}
		return fl;
	}
	
	public static String getFilePath(String filePath){
		return getFilePath(filePath, SystemServer.currentTimeMillis());
	}
	
	public static String getFilePath(String filePath,long time){
		String folder = filePath;
		String formatTime;
		synchronized (dfFile) {
			formatTime = dfFile.format(new Date(time));
		}
		return SystemServer.getInstance().getRootPath("/logs"+File.separator+folder)+File.separator +formatTime+ ".txt";
	}

	private static final String end = "\r\n";
	private static SimpleDateFormat dfFile = new SimpleDateFormat("y-M-d");
	public static final SimpleDateFormat dfLog = new SimpleDateFormat("y-MM-dd HH:mm:ss.sss");
	private static HashMap<String, FileLogger> mapNameToLogger = new HashMap<String, FileLogger>();
	private RecycleObjectPool<Record> mRecordRecycleObjectPool = new RecycleObjectPool<>(3);
	private BufferedWriter mBw;
	private boolean mNeedInit = true;
	private ExecutorService mSingleThreadPoolExecutor;
	private String mFileName;
	private long mDayTime;
	private int mMaxFileNum = 2;
	private boolean mLogcat = SystemServer.IsDebug();

	private FileLogger(String fileName){
		mFileName = fileName;
		mDayTime = SystemServer.currentTimeMillis();
		initLogHandler();
	}

	public FileLogger setLogcat(boolean b){
		mLogcat = b;
		return this;
	}
	
	public FileLogger setMaxFileNum(int maxNum){
		mMaxFileNum = maxNum;
		return this;
	}
	
	private void initWriter(){
		if(mBw == null){
			if(mNeedInit){
				mNeedInit = false;
				String path = getFilePath(mFileName,mDayTime);
				if(FileHelper.checkOrCreateDirectory(path)){
					File f = new File(path);
					File parent = f.getParentFile();
					File childs[] = parent.listFiles();
					if(childs != null && childs.length > mMaxFileNum){
						long minTime = Long.MAX_VALUE;
						File delete = null;
						for(File child : childs){
							if(child.lastModified() < minTime){
								minTime = child.lastModified();
								delete = child;
							}
						}
						if(delete != null){
							delete.delete();
						}
					}
					try{
						mBw = new BufferedWriter(new FileWriter(path,true));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void initLogHandler(){
		mSingleThreadPoolExecutor = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, 
				new LinkedBlockingQueue<Runnable>());
	}
	
	public void log(String format,Object ...args){
		if(args != null){
			format = String.format(format, args);
		}
		log(format);
	}
	
	public void log(String message){
		log(obtainRecord(message));
	}

	public void log(StackTraceElement[] stack){
		log(obtainRecord(stack));
	}
	
	public void log(final Record record){
		if(record.urgent){
			doLog(record);
		}else{
			mSingleThreadPoolExecutor.execute(new Runnable() {
				@Override
				public void run() {
					doLog(record);
				}
			});
		}
	}

	public void log(Throwable e){
		log(obtainRecord(e));
	}
	
	private void doLog(Record record){
		if(mLogcat){
			System.out.print(String.format("%s  ", dfLog.format(new Date(record.logTime))));
			System.out.print(formatRecord(record));
		}

		try {
			long curTime = SystemServer.currentTimeMillis();
			if(!DateUtils.isDateDayEqual(curTime, mDayTime)){
				mDayTime = curTime;
				mNeedInit = true;
				if(mBw != null){
					try{
						mBw.close();
					}catch(Exception e){
						e.printStackTrace();
					}finally{
						mBw = null;
					}
				}
			}
			initWriter();
			if(mBw != null){
				mBw.newLine();
				mBw.write(String.format("%s  ", dfLog.format(new Date(record.logTime))));
				if(record.bytes == null){
					mBw.write(formatRecord(record));
				}else{
					mBw.newLine();
					mBw.write(new String(record.bytes));
				}
				if(record.stackTrack != null){
					for(StackTraceElement ste : record.stackTrack){
						if("setPrintCallStack".equals(ste.getMethodName())){
							continue;
						}
						mBw.newLine();
						mBw.append(ste.getClassName());
						mBw.append('.');
						mBw.append(ste.getMethodName());

				        if (ste.isNativeMethod()) {
				        	mBw.append("(Native Method)");
				        } else {
				            String fName = ste.getFileName();
				            if (fName == null) {
				            	mBw.append("(Unknown Source)");
				            } else {
				                int lineNum = ste.getLineNumber();
				                mBw.append('(');
				                mBw.append(fName);
				                if (lineNum >= 0) {
				                	mBw.append(':');
				                	mBw.append(String.valueOf(lineNum));
				                }
				                mBw.append(')');
				            }
				        }
					}
					mBw.newLine();
				}
				mBw.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(!FileHelper.isFileExists(getFilePath(mFileName, mDayTime))){
				SystemUtils.close(mBw);
				mBw = null;
			}
		}finally {
			mRecordRecycleObjectPool.recycle(record);
		}
	}

	public Record obtainRecord(String message){
		Record record = mRecordRecycleObjectPool.poll();
		if(record!=null){
			record.message = message;
			record.resolve();
			return record;
		}
		return new Record(message);
	}

	public Record obtainRecord(Throwable e){
		Record record = mRecordRecycleObjectPool.poll();
		if(record!=null){
			record.message = e.getMessage();
			record.stackTrack = e.getStackTrace();
			record.resolve();
			return record;
		}
		return new Record(e);
	}

	public String formatRecord(Record record){
		StringBuilder sb = new StringBuilder();
		appendX(sb,String.format("[%s]",record.threadName));
		appendX(sb,record.sourceClassName);
		appendX(sb,record.sourceMethodName);
		if(record.lineNumber>0){
			appendX(sb,String.format("[line:%s]",record.lineNumber));
		}
		sb.append(" ==>> ");

		if(record.stackTrack != null){
			for(StackTraceElement e : record.stackTrack){
				sb.append(e.getClassName())
						.append(end)
						.append(e.getMethodName())
						.append(end);
			}
		}
		if(record.bytes != null){
			sb.append(new String(record.bytes));
		}
		if(!TextUtils.isEmpty(record.message)){
			sb.append(record.message);
		}

		sb.append(end);
		return sb.toString();
	}

	private static void appendX(StringBuilder sb,String append){
		if(append!=null){
			if(sb.length()>0){
				sb.append(":");
			}
			sb.append(append);
		}
	}

	public Record obtainRecord(StackTraceElement[] stack){
		Record record = mRecordRecycleObjectPool.poll();
		if(record!=null){
			record.message="";
			record.stackTrack = stack;
			record.resolve();
			return record;
		}
		return new Record(stack);
	}

	public static class Record implements Recycleable {

		private static List<String> findClasses;
		static{
			findClasses = new ArrayList<>();
			findClasses.add(FileLogger.class.getName());
			findClasses.add(DebugLog.class.getName());
		}

		long logTime;
		String 	message;
		byte bytes[];
		StackTraceElement[] stackTrack;
		boolean urgent;
		String sourceClassName;
		String sourceMethodName;
		int lineNumber;
		String threadName;
		
		public Record(String message) {
			this.message = message;
			resolve();
		}
		
		public Record(byte bytes[],int offset,int count) {
			this.bytes = new byte[count];
			System.arraycopy(bytes, offset, this.bytes, 0, count);
			resolve();
		}
		
		public Record(Throwable e) {
			this.message = e.getMessage();
			stackTrack = e.getStackTrace();
			resolve();
		}
		
		public Record(StackTraceElement[] stack){
			this.message = "";
			stackTrack = stack;
			resolve();
		}

		private boolean isFindClass(String className){
			return findClasses.contains(className);
		}

		private void resolve() {
			setCurrentThreadName();
			StackTraceElement[] stack = new Throwable().getStackTrace();
			String sourceClassName = null;
			String sourceMethodName = null;
			int lineNumber = 0;
			boolean found = false;
			for (StackTraceElement element : stack) {
				String className = element.getClassName();
				if (isFindClass(className)) {
					found = true;
				}
				else if (found) {
					if (isFindClass(className)) {
						continue;
					}
					sourceClassName = className;
					sourceMethodName = element.getMethodName();
					lineNumber = element.getLineNumber();
					break;
				}
			}
			setSourceClassName(sourceClassName);
			setSourceMethodName(sourceMethodName);
			setLineNumber(lineNumber);
			setLogTime(System.currentTimeMillis());
		}

		public void setLogTime(long logTime) {
			this.logTime = logTime;
		}

		public void setCurrentThreadName(){
			setThreadName(Thread.currentThread().getName());
		}

		public void setThreadName(String threadName) {
			this.threadName = threadName;
		}

		public void setSourceClassName(String sourceClassName) {
			this.sourceClassName = sourceClassName;
		}

		public void setSourceMethodName(String sourceMethodName) {
			this.sourceMethodName = sourceMethodName;
		}

		public String getSourceClassName() {
			return sourceClassName;
		}

		public String getSourceMethodName() {
			return sourceMethodName;
		}

		public void setLineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
		}

		public Record setUrgent(){
			this.urgent = true;
			return this;
		}
		
		public Record setPrintCallStack(){
			stackTrack = new Throwable().getStackTrace();
			return this;
		}

		@Override
		public void recycle() {
			message=null;
			bytes = null;
			stackTrack=null;
			urgent = false;
		}
	}
}
