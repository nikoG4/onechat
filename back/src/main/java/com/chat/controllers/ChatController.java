package com.chat.controllers;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chat.models.Chat;
import com.chat.services.ChatService;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    @Autowired
    private ChatService chatService;


    @PostMapping
    public ResponseEntity<Chat> createChat(@RequestBody Chat chat) {
        return ResponseEntity.ok(chatService.createChat(chat));
    }

    @GetMapping
    public ResponseEntity<List<Chat>> getAllChats() {
        return ResponseEntity.ok(chatService.getAllChats());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Chat>> getChatsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getChatsByUser(userId));
    }

    @GetMapping("/search/{query}")
    public ResponseEntity<List<Chat>> getChatsByUserNameOrPhoneNumber(@PathVariable String query) {
        return ResponseEntity.ok(chatService.getChatsByUserNameOrPhoneNumber(query));
    }
}
