package com.xbcx.decoder.config;

import com.xbcx.decoder.web.websocket.WebSocketHandshakeInterceptor;
import com.xbcx.decoder.web.websocket.WebSocketRtspHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketRtspHandler(),"ws/rtsp")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketHandshakeInterceptor());
    }

}