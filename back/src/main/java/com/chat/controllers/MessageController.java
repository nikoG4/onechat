package com.chat.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chat.models.Message;
import com.chat.models.response.MessageResponse;
import com.chat.services.MessageService;



@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;


    @PostMapping
    public ResponseEntity<Message> sendMessage(@RequestBody MessageResponse messageResponse) {


        Message newMessage = new Message();
        newMessage.setId(messageResponse.getId());
        newMessage.setMessage(messageResponse.getMessage());
        newMessage.setDate(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        newMessage.setStatus("sent");
        newMessage.setType(messageResponse.getType());
        newMessage.setMedia(messageResponse.getMedia());
        newMessage.setSender(messageResponse.getSender());
        newMessage.setChat(messageResponse.getChat());

        return ResponseEntity.ok(messageService.sendMessage(newMessage));
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<Message>> getMessagesByChat(@PathVariable Long chatId) {
        return ResponseEntity.ok(messageService.getMessagesByChat(chatId));
    }
}

