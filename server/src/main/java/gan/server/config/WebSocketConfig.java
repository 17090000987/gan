package gan.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import gan.server.web.websocket.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketRtspHandler()
                .registerProtrolHandler("control",new WebSocketHandlerRtspControl())
                .registerProtrolHandler("data",new WebSocketHandlerRtspData()),
                "ws/rtsp")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketHandshakeInterceptor());
        registry.addHandler(new WebSocketFmp4Handler(),"ws/mp4");
        registry.addHandler(new WebSocketH5streamFmp4Handler(), "ws/file");
    }

}