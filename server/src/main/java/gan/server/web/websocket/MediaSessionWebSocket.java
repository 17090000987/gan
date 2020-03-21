package gan.server.web.websocket;

import android.os.Handler;
import gan.log.DebugLog;
import gan.media.MediaSession;
import gan.core.system.SystemUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import gan.server.GanServer;

import java.io.FileOutputStream;
import java.io.IOException;

public class MediaSessionWebSocket implements MediaSession<WebSocketSession> {

    WebSocketSession mWebSocketSession;
    FileOutputStream fos;
    Runnable  mSessionTimeOut = new Runnable() {
        @Override
        public void run() {
            DebugLog.warn("websocket session timeout, id:%s", getSessionId());
            SystemUtils.close(MediaSessionWebSocket.this);
        }
    };

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
        sessionTimer();
    }

    byte[] b1 = new byte[1];
    @Override
    public void sendMessage(int b) throws IOException {
        b1[0] = (byte)b;
        mWebSocketSession.sendMessage(new BinaryMessage(b1));
        sessionTimer();
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        mWebSocketSession.sendMessage(new BinaryMessage(b));
        sessionTimer();
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
        mWebSocketSession.sendMessage(new BinaryMessage(b,off,len, true));
        sessionTimer();
    }

    @Override
    public void close() throws IOException {
        sessionTimerCancel();
        SystemUtils.close(mWebSocketSession);
        SystemUtils.close(fos);
    }

    public void sessionTimer(){
        sessionTimer(60000);
    }

    public final void sessionTimer(long time){
        Handler handler = GanServer.getMainHandler();
        handler.removeCallbacks(mSessionTimeOut);
        handler.postDelayed(mSessionTimeOut, time);
    }

    private void sessionTimerCancel(){
        Handler handler = GanServer.getMainHandler();
        handler.removeCallbacks(mSessionTimeOut);
    }
}
