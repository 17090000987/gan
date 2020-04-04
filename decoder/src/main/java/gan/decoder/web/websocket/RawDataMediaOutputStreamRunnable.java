package gan.decoder.web.websocket;

import gan.log.FileLogger;
import gan.media.MediaInfo;
import gan.media.MediaOutputInfo;
import gan.media.MediaOutputStreamRunnable;
import gan.media.rtsp.RtspMediaServerManager;
import gan.core.system.SystemUtils;

import java.io.FileOutputStream;
import java.io.IOException;

public class RawDataMediaOutputStreamRunnable implements MediaOutputStreamRunnable {

    MediaSessionWebSocket mMediaSessionWebSocket;
    MediaOutputInfo mMediaOutputInfo;
    FileLogger mLogger;

    public RawDataMediaOutputStreamRunnable(MediaOutputInfo mediaOutputInfo, MediaSessionWebSocket sessionWebSocket){
        mMediaSessionWebSocket = sessionWebSocket;
        mMediaOutputInfo = mediaOutputInfo;
        mLogger = RtspMediaServerManager.getLogger(mediaOutputInfo.url);
    }

    @Override
    public String getPacketType() {
        return MediaOutputStreamRunnable.PacketType_Raw;
    }

    @Override
    public MediaInfo getMediaInfo() {
        return mMediaOutputInfo;
    }

    @Override
    public void start() {

    }

    FileOutputStream fos;
    int index;
    @Override
    public void putPacket(byte channel, byte[] packet, int offset, int len, long time) {
//        if(SystemServer.getInstance().isDebug()){
//            try {
//                if(fos==null){
//                    fos = FileHelper.createFileOutputStream(SystemServer.getPublicPath("/file/"+index+".yuv"));
//                }
//                fos.write(packet,offset,len);
//            }catch (Exception e){
//            }
//        }

        try {
            mMediaSessionWebSocket.sendMessage(packet,offset,len);
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void close() {
        SystemUtils.close(mMediaSessionWebSocket);
        SystemUtils.close(fos);
    }

    @Override
    public void run() {

    }

}
