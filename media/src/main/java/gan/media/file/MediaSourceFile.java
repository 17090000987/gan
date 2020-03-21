package gan.media.file;

import gan.core.utils.TextUtils;
import gan.media.*;
import gan.media.h26x.HUtils;
import gan.core.system.SystemUtils;
import gan.core.system.server.SystemServer;
import gan.web.config.MediaConfig;

import java.io.FileInputStream;
import java.nio.ByteBuffer;

public class MediaSourceFile extends MediaSourceAbstract implements Runnable{

    String mUri;

    @Override
    protected void onCreateSession(MediaSession session) {
        super.onCreateSession(session);
        setOutputEmptyAutoFinish(true);
        if(mMediaSession instanceof  MediaSessionFile){
            mUri= ((MediaSessionFile)mMediaSession).getSession();
        }
    }

    @Override
    public String getUri() {
        if(!TextUtils.isEmpty(mUri)){
            return mUri;
        }
        if(mMediaSession instanceof  MediaSessionFile){
            return mUri=((MediaSessionFile)mMediaSession).getSession();
        }
        return null;
    }

    @Override
    public String getMediaCodec() {
        String uri = getUri();
        if(uri.contains("h265")){
            return Media.MediaCodec.CODEC_H265;
        }
        return "video/mp4; codecs="+"avc1.42E01E, mp4a.40.2";
    }

    @Override
    protected void onDestory() {
        super.onDestory();
    }

    @Override
    protected void onStartInput() {
        super.onStartInput();
        String uri = getUri();
        MediaServerManager.getInstance().managerMediaSource(uri,this);
    }

    @Override
    public void stopInput() {
        try{
            super.stopInput();
        }finally {
            MediaServerManager.getInstance().removeMediaSource(getUri());
        }
    }

    ByteBuffer mFrameBuffer;
    byte[] mTempBuffer;
    volatile boolean runing;
    public void start(){
        if(runing){
            return ;
        }
        runing = true;
        MediaConfig mediaConfig = MediaApplication.getMediaConfig();
        mFrameBuffer = ByteBuffer.allocate(mediaConfig.rtspFrameBufferSize);
        mTempBuffer = new byte[mediaConfig.rtspFrameBufferSize];
        SystemServer.executeThread(this);
    }

    @Override
    protected void notifyMediaOutputStreamRunnableChanged(int newAllCount, String packetType, int packetTypeRunnableCount) {
        super.notifyMediaOutputStreamRunnableChanged(newAllCount, packetType, packetTypeRunnableCount);
        if(newAllCount==1){
            start();
        }
    }

    public boolean isRuning() {
        return runing;
    }

    @Override
    public void run() {
        String uri = getUri();
        if(!TextUtils.isEmpty(uri)){
            FileInputStream fis=null;
            try{
                String filePath = SystemServer.getPublicPath(uri);
                fis = new FileInputStream(filePath);
                int len;
                byte[] buf = new byte[4096];
                while (isInputing()
                        && ((len=fis.read(buf))!=-1)){
                    putData(buf,0,len);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                runing = false;
                SystemUtils.close(fis);
            }
        }
    }

    int tempLen;
    private void putData(byte[] packet, int offset,int len){
        putData((byte)0,packet,offset,len, System.currentTimeMillis());
        try{
            mFrameBuffer.put(packet,0,len);
            int frameLen = HUtils.find2frameLen(mFrameBuffer.array(), 0, mFrameBuffer.position());
            if(frameLen>0){
                putFrame((byte)0, mFrameBuffer.array(), 0, frameLen, 0);
                System.arraycopy(mFrameBuffer.array(), frameLen, mTempBuffer, 0, tempLen = (mFrameBuffer.position() - frameLen));
                mFrameBuffer.clear();
                mFrameBuffer.put(mTempBuffer,0, tempLen);
            }
        }catch (Exception e){
            mFrameBuffer.clear();
        }
    }

    public final void putData(byte channel, byte[] packet, int offset,int len,long time){
        outputPacketStream(MediaOutputStreamRunnable.PacketType_None,channel,packet,offset,len,time);
    }

    public final void putFrame(byte channel, byte[] packet, int offset,int len,long time){
        outputPacketStream(MediaOutputStreamRunnable.PacketType_Frame, channel,packet,offset,len,time);
    }

}
