package com.chat.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chat.models.Chat;
import com.chat.models.Message;
import com.chat.repositories.MessageRepository;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private WebSocketService webSocketService;


    public Message sendMessage(Message message) {

        
        if(message.getChat().getId() == null) {

            Chat chat = message.getChat();
            
            if(!chat.isGroup() && chat.getParticipants().size() == 1) {

                chat.setParticipants(List.of(chat.getParticipants().get(0)));

                chat.setCreatedBy(message.getSender());

            }

            message.setChat(chatService.createChat(chat));

        }else{

            message.setChat(chatService.findChatById(message.getChat().getId()));

        }

        Message savedMessage = messageRepository.save(message);
        if (savedMessage.getChat() != null && savedMessage.getChat().getId() != null) {
            webSocketService.broadcastChatMessage(savedMessage.getChat().getId(), savedMessage);
        }

        return savedMessage;

    }

    public List<Message> getMessagesByChat(Long chatId) {
        return messageRepository.findByChatId(chatId);
    }
}
