package gan.media.rtsp;

import gan.core.system.server.SystemServer;
import gan.core.system.server.ServerPlugin;
import gan.media.BufferInfo;
import gan.media.MediaOutputStreamRunnable;
import gan.media.MediaOutputStreamRunnableFrame;
import gan.media.mp4.MediaOutputStreamRunnableMp4;
import gan.media.mp4.Mp4MeidaOutputStream;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Rtsp2Fmp4ServerPlugin extends ServerPlugin<RtspMediaServer> implements RtspMediaServer.OnFrameCallBackPlugin,RtspMediaServer.OnMediaOutputStreamRunnableChangedPlugin {

    MediaOutputStreamRunnableFrame mMediaOutputStreamRunnableFrame;
    byte[] head1,head2;

    @Override
    protected void onCreate(RtspMediaServer server) {
        super.onCreate(server);
        mMediaOutputStreamRunnableFrame = new MediaOutputStreamRunnableFrame(
                new Mp4MeidaOutputStream(server.getUrl(),null){
            @Override
            public void onMp4(byte[] data, int length) {
                super.onMp4(data, length);
                Rtsp2Fmp4ServerPlugin.this.onMp4(data,length);
            }
        }, null){
            @Override
            public void close() {
                super.close();
                mp4End();
            }
        };
        mMediaOutputStreamRunnableFrame.close();
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        mMediaOutputStreamRunnableFrame.close();
    }

    @Override
    public void onFrame(byte channel, ByteBuffer frame, BufferInfo info) {
        mMediaOutputStreamRunnableFrame.putPacket(channel,frame.array(),info.offset, info.length, info.time);
    }

    protected  void onMp4(byte[] data, int length){
        if(head1==null){
            head1 = new byte[length];
            System.arraycopy(data,0, head1, 0, length);
        }else if(head2 == null){
            head2 = new byte[length];
            System.arraycopy(data,0, head2, 0, length);
        }else{
            writeMp4Head(head1,head2);
            mServer.outputPacketStream(MediaOutputStreamRunnable.PacketType_Mp4, (byte)0, data,0, length, 0);
        }
    }

    public final void writeMp4Head(byte[] head1,byte[] head2){
        synchronized (mServer.mOutputStreamRunnables){
            Collection<MediaOutputStreamRunnable> runnables = mServer.mOutputStreamRunnables.get(MediaOutputStreamRunnable.PacketType_Mp4);
            if(runnables.size()>0){
                List<MediaOutputStreamRunnable> errors = new ArrayList<>();
                for(MediaOutputStreamRunnable runnable:runnables){
                    try {
                        if(runnable instanceof MediaOutputStreamRunnableMp4){
                            ((MediaOutputStreamRunnableMp4)runnable).writeHead(head1,head2);
                        }
                    } catch (Exception e) {
                        //ignore
                        e.printStackTrace();
                        runnable.close();
                        errors.add(runnable);
                    }
                }
                mServer.mOutputStreamRunnables.removeAll(errors);
            }
        }
    }

    public void mp4End(){
        head1 = null;
        head2 = null;
    }

    boolean mRuning;
    @Override
    public void onMediaOutputStreamRunnableChanged(int newAllCount, String packetType, int packetTypeRunnableCount) {
        if(MediaOutputStreamRunnable.PacketType_Mp4.equals(packetType)){
            if(packetTypeRunnableCount>0){
                if(mMediaOutputStreamRunnableFrame.isColsed()
                        ||!mRuning){
                    mRuning = true;
                    SystemServer.executeThread(mMediaOutputStreamRunnableFrame);
                }
            }else{
                mRuning = false;
                mMediaOutputStreamRunnableFrame.close();
            }
        }
    }

}
