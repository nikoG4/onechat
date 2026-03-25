package com.chat.services;

import com.chat.models.Message;
import com.chat.models.response.ChatWsEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final ObjectMapper objectMapper;

    // chatId -> sessions subscribed
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> sessionsByChat = new ConcurrentHashMap<>();
    // session -> subscribed chatIds (para limpiar al desconectar)
    private final ConcurrentHashMap<WebSocketSession, Set<Long>> chatsBySession = new ConcurrentHashMap<>();

    public void subscribe(long chatId, WebSocketSession session) {
        sessionsByChat.computeIfAbsent(chatId, ignored -> new CopyOnWriteArraySet<>()).add(session);
        chatsBySession.computeIfAbsent(session, ignored -> ConcurrentHashMap.newKeySet()).add(chatId);
    }

    public void removeSession(WebSocketSession session) {
        Set<Long> chatIds = chatsBySession.remove(session);
        if (chatIds == null) return;

        for (Long chatId : new HashSet<>(chatIds)) {
            CopyOnWriteArraySet<WebSocketSession> sessions = sessionsByChat.get(chatId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByChat.remove(chatId, sessions);
                }
            }
        }
    }

    public void broadcastChatMessage(long chatId, Message message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = sessionsByChat.get(chatId);
        if (sessions == null || sessions.isEmpty()) return;

        ChatWsEvent event = new ChatWsEvent("message", chatId, message);
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) continue;
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException ignored) {
            }
        }
    }
}
