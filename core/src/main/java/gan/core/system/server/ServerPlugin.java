package gan.core.system.server;

public class ServerPlugin<T extends BaseServer> implements ServerListener {

    protected T mServer;

    protected final void onAttachServer(T server){
        mServer = server;
        onCreate(server);
    }

    protected void onCreate(T server){
    }

    protected void onDestory(){

    }

    public T getServer() {
        return mServer;
    }

    public final void finishSelf(){
        mServer.destoryPlugin(this);
    }
}
