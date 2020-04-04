package gan.core.system;

import gan.core.system.server.SystemServer;

import java.io.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SystemUtils {
	private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10,
			Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

	public static void execute(Runnable runnable){
		threadPoolExecutor.execute(runnable);
	}

	public static Object byteArrayToObject(byte[] data) throws StreamCorruptedException, IOException, ClassNotFoundException{
    	if(data == null){
    		return null;
    	}
    	ByteArrayInputStream bais = new ByteArrayInputStream(data);
		ObjectInputStream ois = new ObjectInputStream(bais);
		try{
			return ois.readObject();
		}finally{
			ois.close();
		}
    }

    public static void close(Closeable closeable){
		if(closeable!=null){
			try{
				closeable.close();
			}catch (IOException e){
				//ignore
			}
		}
	}

	public static String map2NetParams(Map<String,String> values){
		StringBuffer sb = new StringBuffer();
		String temp = "";
		for(String key:values.keySet()){
			temp = key+":"+values.get(key)+"\r\n";
			sb.append(temp);
		}
		return sb.toString();
	}

	public static long getProcessId(Process process){
		return -1;
	}

	/**
	 * 不要乱用，确定pid 是正确的，否则在linux平台下可能造成系统异常
	 * @param pid
	 */
	public static void killProcess(String pid){
		try {
			String cmd = "kill -9 "+pid;
			Process killPrcess = Runtime.getRuntime().exec(cmd);
			killPrcess.waitFor();
			killPrcess.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void killProcessByName(String pName){
		try {
			if(isWindows()){
				Runtime.getRuntime().exec("taskkill -f -t -im " + pName + ".exe");
			}else{
				Runtime.getRuntime().exec("killall -9 " + pName);
			}
		} catch (Exception e) {
		}
	}

	public static boolean isWindows(){
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static void killProcess(Process process){
		process.destroy();
	}

	public static void clearByteBuferArray(List<ByteBuffer> buffers){
		for(ByteBuffer buffer:buffers){
			buffer.clear();
		}
		buffers.clear();
	}

	public static int safeParseInt(String s){
		try{
			return Integer.parseInt(s);
		}catch(Exception e){
		}
		return 0;
	}

	public static long safeParseLong(String s, long fallback){
		try {
			return Long.parseLong(s);
		} catch (Exception e) {
			return fallback;
		}
	}

	public static long getUnsignedIntt (int data){     //将int数据转换为0~4294967295 (0xFFFFFFFF即DWORD)。
		return data&0x0FFFFFFFFl;
	}

	public static int byteToUnsignInt8(byte[] b, int offset){
		return ((int)b[offset] & 0xFF);
	}

	public static int byte2UnsignInt8(byte b){
		return (b & 0xFF);
	}

	public static int byteToUnsignInt16(byte[] b, int offset){
		return (byteToUnsignInt8(b, offset) << 8) + (byteToUnsignInt8(b, offset+1));
	}

	public static long byteToUnsignInt32(byte[] b, int offset){
		long raw = ((long)byteToUnsignInt8(b, offset) << 24) +
				((long)byteToUnsignInt8(b, offset+1) << 16) +
				((long)byteToUnsignInt8(b, offset+2) << 8) +
				((long)byteToUnsignInt8(b, offset+3));
		return (raw) & 0xFFFFFFFFL;
	}

	public static long byteToUnsignInt64(byte[] b, int offset){
		long raw = ((long)byteToUnsignInt8(b, offset) << 56) +
				((long)byteToUnsignInt8(b, offset+1) << 48) +
				((long)byteToUnsignInt8(b, offset+2) << 40) +
				((long)byteToUnsignInt8(b, offset+3) << 32) +
				((long)byteToUnsignInt8(b, offset+4) << 24) +
				((long)byteToUnsignInt8(b, offset+5) << 16) +
				((long)byteToUnsignInt8(b, offset+6) << 8) +
				((long)byteToUnsignInt8(b, offset+7));
		return (raw) & 0xFFFFFFFFFFFFFFFFL;
	}

	public static int readInputStream(InputStream inputStream, byte[] buf, int length) throws IOException{
		if(inputStream != null && buf != null && length > 0){
			int readOffset = 0;
			do{
				int readLength = inputStream.read(buf, readOffset, length - readOffset);
				if(readLength < 0){
					return readLength;
				}else{
					readOffset += readLength;
				}
			}while(readOffset < length);
			return readOffset;
		}else{
			return -1;
		}
	}

	public static String dumpObject(Object obj){
		if(obj != null){
			if(obj instanceof Iterable){
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				if(obj instanceof Iterable){
					Iterable<?> iterable = (Iterable<?>)obj;
					for (Object object : iterable) {
						if(sb.length() > 1){
							sb.append(",");
						}
						sb.append(dumpObjectInternal(object));
					}
				}
				sb.append("]");
				return sb.toString();
			}else{
				return dumpObjectInternal(obj);
			}
		}else{
			return "\"null\"";
		}
	}
	
	private static String dumpObjectInternal(Object obj){
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		if(obj != null){
			if(obj instanceof Map){
				@SuppressWarnings("unchecked")
				Map<Object, Object> map = (Map<Object, Object>)obj;
				Set<Map.Entry<Object, Object>> set = map.entrySet();
				for(Map.Entry<Object, Object> entry : set){
					if(sb.length() > 1){
						sb.append(",");
					}
					sb.append("\""+entry.getKey()+"\":\""+entry.getValue()+"\"");
				}
			}else{
				Class<?> clazz = obj.getClass();
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					try {
						if(sb.length() > 1){
							sb.append(",");
						}
						field.setAccessible(true);
						String fieldName = field.getName();
						Class<?> fieldClass = field.getType();
						if(fieldClass.equals(int.class)){
							sb.append("\""+fieldName+"\":\""+field.getInt(obj)+"\"");
						}else if(fieldClass.equals(float.class)){
							sb.append("\""+fieldName+"\":\""+field.getFloat(obj)+"\"");
						}else if(fieldClass.equals(double.class)){
							sb.append("\""+fieldName+"\":\""+field.getDouble(obj)+"\"");
						}else if(fieldClass.equals(long.class)){
							sb.append("\""+fieldName+"\":\""+field.getLong(obj)+"\"");
						}else if(fieldClass.equals(boolean.class)){
							sb.append("\""+fieldName+"\":\""+field.getBoolean(obj)+"\"");
						}else if(fieldClass.equals(String.class)){
							String str = (String) field.get(obj);
							if(str != null){
								str = str.replaceAll("\\\"", "\\\\\"");
							}
							sb.append("\""+fieldName+"\":\""+str+"\"");
						}else{
							Dump[] dump = field.getAnnotationsByType(Dump.class);
							if(dump != null && dump.length > 0){
								sb.append("\""+fieldName+"\":"+dumpObject(field.get(obj)));
							}else{
								sb.append("\""+fieldName+"\":\""+field.get(obj)+"\"");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		sb.append("}");
		return sb.toString();
	}
	
	public static String throwable2String(Throwable ex){
		Writer writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		Throwable cause = ex.getCause();
		// 循环着把所有的异常信息写入writer中
		while (cause != null) {
			cause.printStackTrace(pw);
			cause = cause.getCause();
		}
		pw.close();// 记得关闭
		return writer.toString();
	}

	public static <T extends Object> T safeJsonValues(String json,Class<T> cls){
		try{
			return SystemServer.getObjectMapper().readValue(json,cls);
		}catch (Exception e){
		}
		return null;
	}
	
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Dump{
		
	}

	public static boolean isMainThread(){
		return "main".equals(Thread.currentThread().getName());
	}

	public static Method getMethod(Class<?> cls, String methodName, Class<?>... parameterTypes){
		for(Class<?> c = cls;c != null;c = c.getSuperclass()){
			try{
				Method m = c.getDeclaredMethod(methodName,parameterTypes);
				m.setAccessible(true);
				return m;
			}catch(Exception e){
			}
		}
		return null;
	}
}
