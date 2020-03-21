package gan.server.web.websocket;

import gan.core.system.server.SystemServer;
import gan.core.file.FileHelper;
import gan.core.system.SystemUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.FileInputStream;

public class WebSocketFmp4Handler extends AbstractWebSocketHandler {

    final static String Tag = WebSocketFmp4Handler.class.getName();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String fileName = new String(message.asBytes(),"UTF-8");
        String filePath = SystemServer.getRootPath("/media/video/"+fileName);
        if(!FileHelper.isFileExists(filePath)){
            session.sendMessage(new TextMessage("找不到文件"));
            return;
        }

        FileInputStream fileInputStream = new FileInputStream(filePath);
        byte[] buf = new byte[1024];
        int len=0;
        while ((len = fileInputStream.read(buf))!=-1){
            session.sendMessage(new BinaryMessage(buf,0,len,true));
            Thread.sleep(10);
        }
        SystemUtils.close(fileInputStream);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
