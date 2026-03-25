package com.chat.models.response;

import com.chat.models.Chat;
import com.chat.models.User;
import lombok.Data;

@Data
public class MessageResponse {
    
    private Long id;

    private String message;

    private String type;

    private String media;

    private User sender; 

    private Chat chat;


}
