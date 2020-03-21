package gan.media;

import gan.media.MediaSession;

import java.io.IOException;
import java.util.UUID;

public class MediaSessionString implements MediaSession<String> {

    String mSessionId;
    String mSession;

    public MediaSessionString(String session){
        mSessionId = formatId(session);
        mSession = session;
    }

    public final static String formatId(String id){
        return "session_string_"+id+"_"+UUID.randomUUID().toString();
    }

    @Override
    public String getSessionId() {
        return mSessionId;
    }

    @Override
    public String getSession() {
        return mSession;
    }

    @Override
    public void sendMessage(String message) throws IOException {
        throw  new UnsupportedOperationException("unsupported sendMessage");
    }

    @Override
    public void sendMessage(int b) throws IOException {
        throw  new UnsupportedOperationException("unsupported sendMessage");
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        sendMessage(b,0, b.length);
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
        throw  new UnsupportedOperationException("unsupported sendMessage");
    }

    @Override
    public void close() throws IOException {

    }

}
