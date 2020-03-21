package gan.server.web.websocket;

import gan.log.DebugLog;
import gan.network.MapValueBuilder;
import gan.network.NetParamsMap;
import gan.core.system.SystemUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class WebSocketHandlerRtspData extends AbstractWebSocketHandler {

    final static String Tag = WebSocketHandlerRtspData.class.getSimpleName();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        final String msg = new String(message.asBytes(),"UTF-8");
        DebugLog.info("handleTextMessage:"+ msg);
        NetParamsMap params = new NetParamsMap();
        String fun = WebSocketServer.parseRequest(msg,params);
        if (fun.contains("JOIN")) {
            StringBuffer sb = new StringBuffer();
            sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                    .put("CSeq", params.get("CSeq"))
                    .build()));
            WebSocketServer.responseRequest(session,200, sb.toString(),null);
        }
    }

}
