package com.chat.models.response;

import com.chat.models.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatWsEvent {
    private String type;
    private Long chatId;
    private Message message;
}

