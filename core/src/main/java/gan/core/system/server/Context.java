package gan.core.system.server;

public class Context {

    public static <T extends BaseServer> T startServer(Class<T> cls,Object... paramster){
        return ServerManager.getInstance().startServer(cls,paramster);
    }

}
