package gan.media.rtsp;

import android.os.Handler;
import gan.core.system.SystemUtils;
import gan.core.system.server.BaseServer;
import gan.core.system.server.SystemServer;

import java.io.IOException;

public class RtspPullServer extends BaseServer implements Runnable{

    RtspConnection mRtspConnection;
    boolean mPlaying;
    String mRtsp;

    public void play(String rtsp){
        mRtsp = rtsp;
        mRtspConnection = new RtspConnection(rtsp);
        SystemServer.executeThread(new Runnable() {
            @Override
            public void run() {
                RtspMediaServer server=null;
                try{
                    mRtspConnection.connect();
                    mRtspConnection.sendRequestOption();
                    RtspConnection.Response response =mRtspConnection.sendRequestDESCRIBE();
                    if(response.status==200){
                        if(mRtspConnection.sendRequestSetup2()){
                            if( mRtspConnection.sendRequestPlay()){
                                mPlaying = true;
                                SystemServer.getMainHandler().removeCallbacks(RtspPullServer.this);
                                server = RtspMediaServerManager.getInstance().addSession(new RtspConnectionMediaSession(mRtspConnection));
                                if(server!=null){
                                    server.startInputStream(rtsp, response.content, mRtspConnection.mInputStream);
                                    RtspMediaServerManager.getInstance().removeSession(server.getSession());
                                }
                            }else{
                                SystemUtils.close(mRtspConnection);
                            }
                        }else{
                            SystemUtils.close(mRtspConnection);
                        }
                    }else{
                        SystemUtils.close(mRtspConnection);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    mPlaying = false;
                    if(server!=null){
                        server.finish();
                    }
                    Handler handler = SystemServer.getMainHandler();
                    handler.removeCallbacks(RtspPullServer.this);
                    handler.postDelayed(RtspPullServer.this,10000);
                }
            }
        });
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        SystemUtils.close(mRtspConnection);
        Handler handler = SystemServer.getMainHandler();
        handler.removeCallbacks(RtspPullServer.this);
    }

    @Override
    public void run() {
        play(mRtsp);
    }

    public void close(){
        mPlaying = false;
    }

    @Override
    public void sendMessage(int b) throws IOException {
        mRtspConnection.send(b);
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        mRtspConnection.send(b);
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
        mRtspConnection.send(b,off,len);
    }

}
