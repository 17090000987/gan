package gan.media.rtsp;

import gan.media.MediaSession;

import java.io.IOException;
import java.util.UUID;

public class RtspConnectionMediaSession implements MediaSession<RtspConnection> {

    RtspConnection mRtspConnection;

    public RtspConnectionMediaSession(RtspConnection rtspConnection){
        mRtspConnection = rtspConnection;
    }

    public final static String formatId(int port){
        return "session_rtsp_"+port+"_"+UUID.randomUUID().toString();
    }

    @Override
    public String getSessionId() {
        return formatId(mRtspConnection.mSocket.getLocalPort());
    }

    @Override
    public RtspConnection getSession() {
        return mRtspConnection;
    }

    public void sendRtspRequest(String rtsp) throws IOException {
        mRtspConnection.sendRtspRequest(rtsp);
    }

    @Override
    public void sendMessage(String message) throws IOException {
        mRtspConnection.send(message.getBytes(RtspMediaServer.charsetName));
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

    @Override
    public void close() throws IOException {
        mRtspConnection.close();
    }

}
