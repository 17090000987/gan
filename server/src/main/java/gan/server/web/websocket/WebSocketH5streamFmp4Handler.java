package gan.server.web.websocket;

import gan.core.system.server.SystemServer;
import gan.core.file.FileHelper;
import gan.core.system.SystemUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.FileOutputStream;

public class WebSocketH5streamFmp4Handler extends AbstractWebSocketHandler {

    final static String Tag = WebSocketH5streamFmp4Handler.class.getName();

    FileOutputStream mFileOut;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String filePath = SystemServer.getRootPath("/media/video/h5_"+session.getId()+".fm4");
            mFileOut = FileHelper.createFileOutputStream(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        SystemUtils.close(mFileOut);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
        mFileOut.write(message.getPayload().array());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
