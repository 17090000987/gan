package gan.media;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class MediaSessionSocket implements MediaSession<Socket>{

    String mSessionId;
    Socket mSocket;

    public MediaSessionSocket(Socket socket){
        this(formatId(socket.getPort()),socket);
    }

    public MediaSessionSocket(String sessionId,Socket socket){
        mSessionId = sessionId;
        mSocket = socket;
    }

    public final static String formatId(int port){
        return "session_socket_"+port+"_"+UUID.randomUUID().toString();
    }

    @Override
    public Socket getSession() {
        return mSocket;
    }

    @Override
    public String getSessionId() {
        return mSessionId;
    }

    @Override
    public void sendMessage(String message) throws IOException {
        sendMessage(message.getBytes("UTF-8"));
    }

    @Override
    public void sendMessage(int b) throws IOException {
        mSocket.getOutputStream().write(b);
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        mSocket.getOutputStream().write(b);
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException{
        mSocket.getOutputStream().write(b);
    }

    @Override
    public void close() throws IOException {
        mSocket.close();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Socket){
            return mSocket.equals(obj);
        }
        return super.equals(obj);
    }
}
