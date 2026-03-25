package com.chat.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime date = LocalDateTime.now();

    private String status = "sent";

    private String type;

    private String media;

    @ManyToOne
    private User sender; 

    @ManyToOne
    @JsonIgnore
    private Chat chat;

    // Expose chat id for clients without serializing the full Chat object.
    @JsonProperty("chatId")
    public Long getChatId() {
        return chat != null ? chat.getId() : null;
    }

}
