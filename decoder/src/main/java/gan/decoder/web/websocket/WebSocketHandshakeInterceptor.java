package gan.decoder.web.websocket;

import gan.core.utils.TextUtils;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();
        HttpServletResponse httpResponse = ((ServletServerHttpResponse) response).getServletResponse();
        final String SecWebSocketProtocol = httpRequest.getHeader("Sec-WebSocket-Protocol");
        if(!TextUtils.isEmpty(SecWebSocketProtocol)){
            httpResponse.addHeader("Sec-WebSocket-Protocol",SecWebSocketProtocol);
        }
        super.afterHandshake(request,response,wsHandler,exception);
    }

}
