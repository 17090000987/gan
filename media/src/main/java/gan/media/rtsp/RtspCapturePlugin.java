package gan.media.rtsp;

import gan.media.BufferInfo;
import gan.media.MediaCaptureCallBack;
import gan.media.h264.H264Utils;
import gan.core.system.server.ServerPlugin;
import gan.core.system.server.SystemServer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RtspCapturePlugin extends ServerPlugin<RtspMediaServer> implements RtspMediaServer.OnFrameCallBackPlugin {

    MediaCaptureCallBack mediaCaptureCallBack;

    Runnable timeOut = new Runnable() {
        @Override
        public void run() {
            runing.set(false);
            mediaCaptureCallBack.onCapture(null,0,0);
            finishSelf();
        }
    };

    private AtomicBoolean runing = new AtomicBoolean(false);

    public RtspCapturePlugin(MediaCaptureCallBack callBack){
        mediaCaptureCallBack = callBack;
    }

    @Override
    protected void onCreate(RtspMediaServer server) {
        super.onCreate(server);
        SystemServer.getMainHandler().postDelayed(timeOut,3000);
        runing.set(true);
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        SystemServer.getMainHandler().removeCallbacks(timeOut);
        if(runing.getAndSet(false)){
            mediaCaptureCallBack.onCapture(null,0,0);
        }
    }

    @Override
    public void onFrame(byte channel, ByteBuffer frame, BufferInfo bufferInfo) {
        synchronized (this){
            BufferInfo info = bufferInfo.clone();
            if(H264Utils.isSPSOrPPS(frame.array(),info.offset,100)){
                H264Utils.offsetSpsPps(frame,info);
            }
            if(H264Utils.isIFrame(frame.array(),info.offset,10)){
                SystemServer.getMainHandler().removeCallbacks(timeOut);
                runing.set(false);
                mediaCaptureCallBack.onCapture(frame.array(),info.offset,info.length);
                finishSelf();
            }
        }
    }

}
