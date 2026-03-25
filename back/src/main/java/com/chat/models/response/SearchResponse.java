package com.chat.models.response;

import java.util.List;

import com.chat.models.Chat;
import com.chat.models.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResponse {
    List<User> users;
    List<Chat> chats;    
}
