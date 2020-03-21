package gan.core.system.server;

import android.os.Handler;
import gan.core.BaseListener;
import gan.core.system.SystemUtils;

public class ServerManager implements BaseListener {

    private final static ServerManager sInstance;

    static{
        SystemServer.addManager(sInstance = new ServerManager());
    }

    protected static ServerManager getInstance(){
        return sInstance;
    }

    protected <T extends BaseServer> T startServer(Class<T> cls,Object... paramster){
        try {
            BaseServer server = cls.newInstance();
            server.create(paramster);
            return (T)server;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static Runnable mGcRunnable = new Runnable() {
        @Override
        public void run() {
            System.gc();
        }
    };

    public static void destoryServer(BaseServer server){
        if(server == null){
            new IllegalArgumentException("server null");
        }
        server.destory();
        runGc();
    }

    private static void runGc(){
        Handler mainHandler = SystemServer.getMainHandler();
        mainHandler.removeCallbacks(mGcRunnable);
        mainHandler.postDelayed(mGcRunnable,3000);
    }

    public static void assetMainThread(){
        if(!SystemUtils.isMainThread()){
            throw new MainThreadException();
        }
    }

    private static class MainThreadException extends Error{
        public MainThreadException(){
            super("main thread exception");
        }
    }

}
