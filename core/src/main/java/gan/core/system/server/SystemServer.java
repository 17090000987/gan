package gan.core.system.server;

import android.os.Handler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gan.core.BaseListener;
import gan.log.DebugLog;
import gan.core.PluginHelper;
import gan.core.SyncPluginHelper;
import gan.core.file.FileHelper;
import gan.core.file.SharePerference;
import gan.core.system.SystemUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.WeakHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SystemServer {

	private final static String TAG = SystemServer.class.getName();

	protected static SystemServer sInstance;

	public SystemServer(){
		sInstance = this;
	}

	private static PluginHelper<BaseListener> pluginHelper = new SyncPluginHelper<BaseListener>();

	public static SystemServer getInstance() {
		return sInstance;
	}
	private static ApplicationContext sContext;
	private static String mRootPath;
	private static long sStartTime;
	private WeakHashMap<String,SharePerference> mMapSharePerference;
	private static ThreadPoolExecutor executorService;
    private static Handler mMainHandler;
    private static ObjectMapper mObjectMapper;

    public final void create(ApplicationContext context){
    	onCreate(context);
	}

	protected void onCreate(ApplicationContext context) {
		sStartTime = System.currentTimeMillis();
		sContext = context;
        mMainHandler = new Handler();
		mRootPath = new File("").getAbsolutePath();
		DebugLog.info("rootPath:"+mRootPath);
		final String libPath = getRootPath("/libs");
		final String libWindows = getRootPath("/libs/windows");
		if(FileHelper.checkOrCreateFolder(libPath)){
            try {
                addLibraryDir(libPath,libWindows);
            } catch (Exception e) {
                e.printStackTrace();
                DebugLog.warn("addLibraryDir e:"+e.getMessage());
            }
        }

		executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
				60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());
		
		addSimpleObjectMapperConfiger();
	}

    public static Handler getMainHandler() {
        return mMainHandler;
    }

	public static ApplicationContext getContext() {
		return sContext;
	}

	public static void addManager(BaseListener manager) {
		pluginHelper.addManager(manager);
	}
	
	public static <T extends BaseListener> Collection<T> getManagers(
			Class<T> cls) {
		return pluginHelper.getManagers(cls);
	}
	
	public static void removeManager(Object manager){
		pluginHelper.removeManager(manager);
	}
	
	public static PluginHelper<BaseListener> getPluginHelper(){
		return pluginHelper;
	}
	
	public static long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	public static String getRootPath(){
		if(mRootPath == null){
			mRootPath = new File("").getAbsolutePath();
		}
		return mRootPath;
	}

	public static String getRootPath(String path){
		return new File(mRootPath+File.separator+path).getAbsolutePath();
	}

	public static String getAssets(){
		return getRootPath("/assets");
	}

	public static String getAssets(String path){
		return getRootPath("/assets"+path);
	}

	public static String getPublicPath(String path){
		if(path.startsWith("/")){
			return getRootPath("/public"+path);
		}
		return getRootPath("/public/"+path);
	}

	public static long getStartTime(){
		return sStartTime;
	}
	
	public SharePerference getSharePerference(String name){
		if(mMapSharePerference == null){
			mMapSharePerference = new WeakHashMap<>();
		}
		SharePerference sharePerference = mMapSharePerference.get(name);
		if(sharePerference==null){
			try {
				sharePerference = new SharePerference(name);
				mMapSharePerference.put(name,sharePerference);
				return sharePerference;
			} catch (IOException e) {
				e.printStackTrace();
				DebugLog.warn(e.getMessage());
				return null;
			}
		}else{
			return sharePerference;
		}
	}

	public static void copyResource(String resource){
		DebugLog.info("copying resource wait for..");
		FileHelper.copyResource(resource, getRootPath(resource));
	}

	public static void executeThread(Runnable runnable){
		executorService.execute(runnable);
	}

	/**
	 * instead of executeThread;
	 * @param run
	 */
	@Deprecated
	public static void runOnBackground(Runnable run){
		executorService.execute(run);
	}

	protected static void addLibraryDir(String... libraryPaths) throws Exception {
		Field userPathsField = ClassLoader.class.getDeclaredField("usr_paths");
		userPathsField.setAccessible(true);
		String[] paths = (String[]) userPathsField.get(null);
		StringBuilder sb = new StringBuilder();
		String suffix = SystemUtils.isWindows()?";":":";
		for (int i = 0; i < paths.length; i++) {
			sb.append(paths[i]).append(suffix);
		}
		for(String libraryPath:libraryPaths){
			if(sb.indexOf(libraryPath)<0){
				sb.append(libraryPath).append(suffix);
			}
		}
		DebugLog.info("addLibraryDir path:%s", sb.toString());
		System.setProperty("java.library.path", sb.toString());
		final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
		sysPathsField.setAccessible(true);
		sysPathsField.set(null, null);
	}

	public static <T extends BaseServer> T startServer(Class<T> cls, Object... paramster){
		return ServerManager.getInstance().startServer(cls,paramster);
	}

	public static boolean IsDebug(){
		return getInstance().isDebug();
	}

	public boolean isDebug(){
		return false;
	}

	public static void runOnMainThread(Runnable runnable){
	    mMainHandler.post(runnable);
    }

	public static ObjectMapper getObjectMapper() {
		if(mObjectMapper==null){
			mObjectMapper = new ObjectMapper();
			for(ObjectMapperConfiger configer:getManagers(ObjectMapperConfiger.class)){
				configer.onConfigObjectMapper(mObjectMapper);
			}
		}
		return mObjectMapper;
	}

	public static void addSimpleObjectMapperConfiger(){
		addManager(new SimpleObjectMapperConfiger());
	}

	public static void removeSimpleObjectMapperConfiger(){
		ArrayList<SimpleObjectMapperConfiger> configers = new ArrayList<>();
		for(SimpleObjectMapperConfiger configer:getManagers(SimpleObjectMapperConfiger.class)){
			configers.add(configer);
		}
		for(SimpleObjectMapperConfiger configer:configers){
			removeManager(configer);
		}
	}

	public static interface ObjectMapperConfiger extends BaseListener{
		public void onConfigObjectMapper(ObjectMapper objectMapper);
	}

	public static class SimpleObjectMapperConfiger implements ObjectMapperConfiger{
		@Override
		public void onConfigObjectMapper(ObjectMapper objectMapper) {
			//序列化的时候序列对象的所有属性
			objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
			//反序列化的时候如果多了其他属性,不抛出异常
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			//如果是空对象的时候,不抛异常
			objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		}
	}
}
