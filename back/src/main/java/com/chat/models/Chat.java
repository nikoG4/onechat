package com.chat.models;


import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chats")
@Data
public class Chat{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private boolean isGroup;

    private Integer messagePendingCount;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String avatar;

    @ManyToOne
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "chat_user",
        joinColumns = @JoinColumn(name = "chat_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;


    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages;

    
    private LocalDateTime createdAt = LocalDateTime.now();

}