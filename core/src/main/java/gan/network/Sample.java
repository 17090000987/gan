package gan.network;

import gan.core.BaseListener;
import gan.log.DebugLog;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class Sample implements BaseListener,SocketListener{

    private final static String Tag = Sample.class.getName();

    private SocketGroupManager mSocketGroupManager;
    private AtomicInteger  mSessionCount = new AtomicInteger(0);
    private Object sourceLock = new Object();
    private AtomicInteger  mSourceCount = new AtomicInteger(0);

    private Sample(){
        //rtsp
        try {
            int port = 8000;
            DebugLog.info("rtspServer start");
            mSocketGroupManager = new SocketGroupManager(Integer.valueOf(port), this);
            mSocketGroupManager.start();
        } catch (IOException e) {
            e.printStackTrace();
            DebugLog.warn("RtspServer start fail:"+e.getMessage());
            return;
        }
    }

    public void initServer(){
    }

    public void destory(){
        onDestory();
    }

    protected void onDestory(){
        if(mSocketGroupManager !=null){
            mSocketGroupManager.close();
        }
    }

    @Override
    public void onSocketCreate(Socket socket) {
        DebugLog.info("onSocketCreate :"+socket.getInetAddress().getHostAddress()+",port:"+socket.getPort());
    }

    @Override
    public void onSocketStream(Socket socket) throws IOException {
        DebugLog.debug("onSocketStream:"+socket.getInetAddress().getHostAddress()+",port:"+socket.getPort());
    }

    @Override
    public void onSocketClosed(Socket socket) {
        DebugLog.info("onSocketClosed"+socket.getInetAddress().getHostAddress()+",port:"+socket.getPort());
    }

}
