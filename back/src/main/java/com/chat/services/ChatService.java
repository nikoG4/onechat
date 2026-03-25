package com.chat.services;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.chat.models.Chat;
import com.chat.repositories.ChatRepository;

import jakarta.transaction.Transactional;


@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    public Chat createChat(Chat chat) {
        return chatRepository.save(chat);
    }

    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }

    @Transactional
    public List<Chat> getChatsByUser(Long userId) {

        List <Chat> chats = chatRepository.findChatsByUser(userId);

        chats.forEach(chat -> {
            Hibernate.initialize(chat.getParticipants());
            Hibernate.initialize(chat.getMessages());

            chat.getMessages().sort((chat1, chat2) -> chat1.getDate().compareTo(chat2.getDate()));
        });

        return  chats;
    }

    public Chat findChatById(Long id) {
        return chatRepository.findById(id).orElse(null);
    }

    public List<Chat> getChatsByUserNameOrPhoneNumber(String query) {
        return chatRepository.findByNameOrPhoneNumber(query);
    }
}
