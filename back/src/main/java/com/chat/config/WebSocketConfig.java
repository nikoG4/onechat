package com.chat.config;

// import java.util.List;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.messaging.converter.MessageConverter;
// import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
// import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
// import org.springframework.messaging.simp.config.ChannelRegistration;
// import org.springframework.messaging.simp.config.MessageBrokerRegistry;
// import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
// import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
// import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
// import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

// @Configuration
// @EnableWebSocketMessageBroker
// public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//     @Override
//     public void configureMessageBroker(MessageBrokerRegistry config) {
//         config.enableSimpleBroker("/topic");
//         config.setApplicationDestinationPrefixes("/app");
//     }

//     @Override
//     public void registerStompEndpoints(StompEndpointRegistry registry) {
//         registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
//     }

//     @Override
//     public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
//         // TODO Auto-generated method stub
//         throw new UnsupportedOperationException("Unimplemented method 'configureWebSocketTransport'");
//     }

//     @Override
//     public void configureClientInboundChannel(ChannelRegistration registration) {
//         // TODO Auto-generated method stub
//         throw new UnsupportedOperationException("Unimplemented method 'configureClientInboundChannel'");
//     }

//     @Override
//     public void configureClientOutboundChannel(ChannelRegistration registration) {
//         // TODO Auto-generated method stub
//         throw new UnsupportedOperationException("Unimplemented method 'configureClientOutboundChannel'");
//     }

//     @Override
//     public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
//         // TODO Auto-generated method stub
//         throw new UnsupportedOperationException("Unimplemented method 'addArgumentResolvers'");
//     }

//     @Override
//     public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
//         // TODO Auto-generated method stub
//         throw new UnsupportedOperationException("Unimplemented method 'addReturnValueHandlers'");
//     }

//     @Override
//     public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
//         // TODO Auto-generated method stub
//         throw new UnsupportedOperationException("Unimplemented method 'configureMessageConverters'");
//     }


// }

import com.chat.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Value("${app.allowed.origins:*}")
    private String[] allowedOrigins;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws")
                .setAllowedOrigins(allowedOrigins);
    }
}
