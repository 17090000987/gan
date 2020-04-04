package gan.decoder.web.websocket;

import gan.core.system.SystemUtils;
import gan.core.system.server.ServerSession;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.FileOutputStream;
import java.io.IOException;

public class MediaSessionWebSocket implements ServerSession<WebSocketSession> {

    WebSocketSession mWebSocketSession;
    FileOutputStream fos;

    public MediaSessionWebSocket(WebSocketSession webSocketSession){
        mWebSocketSession = webSocketSession;
    }

    @Override
    public String getSessionId() {
        return mWebSocketSession.getId();
    }

    @Override
    public WebSocketSession getSession() {
        return mWebSocketSession;
    }

    @Override
    public void sendMessage(String message) throws IOException {
        mWebSocketSession.sendMessage(new TextMessage(message));
    }

    byte[] b1 = new byte[1];
    @Override
    public void sendMessage(int b) throws IOException {
        b1[0] = (byte)b;
        mWebSocketSession.sendMessage(new BinaryMessage(b1));
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        mWebSocketSession.sendMessage(new BinaryMessage(b));
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
        mWebSocketSession.sendMessage(new BinaryMessage(b,off,len, true));
    }

    @Override
    public void close() throws IOException {
        SystemUtils.close(mWebSocketSession);
        SystemUtils.close(fos);
    }
}
