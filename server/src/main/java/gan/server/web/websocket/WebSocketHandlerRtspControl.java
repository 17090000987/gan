package gan.server.web.websocket;

import gan.log.DebugLog;
import gan.network.MapValueBuilder;
import gan.network.NetParamsMap;
import gan.core.system.SystemUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class WebSocketHandlerRtspControl extends TextWebSocketHandler{

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        final String msg = new String(message.asBytes(),"UTF-8");
        DebugLog.info("handleTextMessage:"+ msg);
        NetParamsMap params = new NetParamsMap();
        String fun = WebSocketServer.parseRequest(msg,params);
        if (fun.contains("INIT")) {
            onHandleRequestINIT(session,msg,params);
        }else if(fun.contains("WRAP")){

        }
    }

    protected void onHandleRequestINIT(WebSocketSession session,String request,NetParamsMap params) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                .put("CSeq", params.get("CSeq"))
                .put("channel",0)
                .build()));
        WebSocketServer.responseRequest(session,200, sb.toString(),null);
    }

    protected void onHandleRequestWRAP(WebSocketSession session,String request,NetParamsMap params){
        int contentLength = Integer.valueOf(params.get("contentLength"));
        String content = request.substring(request.length()-contentLength);

        StringBuffer sb = new StringBuffer();
        sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                .put("CSeq", params.get("CSeq"))
                .put("channel",0)
                .build()));
    }
}
