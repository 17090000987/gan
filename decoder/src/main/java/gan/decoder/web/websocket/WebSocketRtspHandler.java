package gan.decoder.web.websocket;

import gan.log.DebugLog;
import gan.core.utils.TextUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketRtspHandler extends AbstractWebSocketHandler {

    ConcurrentHashMap<String,WebSocketHandler> mMapProtrolHandler = new ConcurrentHashMap<>();

    public WebSocketRtspHandler registerProtrolHandler(String protrol, WebSocketHandler handler){
        mMapProtrolHandler.put(protrol,handler);
        return this;
    }

    public void unregisterProtrolHandler(String protrol){
        mMapProtrolHandler.remove(protrol);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketServerManager.getLogger().log("onOpen:%s",session.getId());
        WebSocketHandler handler = findWebSocketHandler(session);
        if(handler!=null){
            handler.afterConnectionEstablished(session);
        }else{
            WebSocketServerManager.getsInstance().onOpen(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        WebSocketServerManager.getLogger().log("onError:"+session.getId());
        WebSocketHandler handler = findWebSocketHandler(session);
        if(handler!=null){
            handler.handleTransportError(session,exception);
        }else {
            WebSocketServerManager.getsInstance().onError(session, exception);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        WebSocketServerManager.getLogger().log("onClose:"+session.getId());
        WebSocketHandler handler = findWebSocketHandler(session);
        if(handler!=null){
            handler.afterConnectionClosed(session,closeStatus);
        }else{
            WebSocketServerManager.getsInstance().onClose(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        DebugLog.debug("session:"+session.getId());
        WebSocketHandler handler = findWebSocketHandler(session);
        if(handler!=null){
            handler.handleMessage(session,message);
        }else{
            WebSocketServerManager.getsInstance().onTextMessage(new String(message.asBytes(),"UTF-8"),session);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
        DebugLog.debug("session:"+session.getId());
        WebSocketHandler handler = findWebSocketHandler(session);
        if(handler!=null){
            handler.handleMessage(session,message);
        }else{
            WebSocketServerManager.getsInstance().onBinaryMessage(message,session);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public WebSocketHandler findWebSocketHandler(WebSocketSession session){
        final String Sec_WebSocket_Protocol = getHttpHeader(session,"Sec-WebSocket-Protocol");
        if(!TextUtils.isEmpty(Sec_WebSocket_Protocol)){
            return mMapProtrolHandler.get(Sec_WebSocket_Protocol);
        }
        return null;
    }

    public String getHttpHeader(WebSocketSession session,String key){
        List<String> values = session.getHandshakeHeaders().get(key);
        if(values!=null){
            return values.get(0);
        }
        return null;
    }
}
