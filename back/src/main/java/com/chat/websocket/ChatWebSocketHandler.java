package com.chat.websocket;

import com.chat.services.WebSocketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        webSocketService.removeSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.has("type") ? root.get("type").asText() : null;

        if ("subscribe".equals(type)) {
            if (!root.has("chatId")) return;
            long chatId = root.get("chatId").asLong();
            webSocketService.subscribe(chatId, session);
        }
    }
}

