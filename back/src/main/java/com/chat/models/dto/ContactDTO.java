package com.chat.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class ContactDTO {
    private Long id;
    private String name;
    private String avatar;
}

