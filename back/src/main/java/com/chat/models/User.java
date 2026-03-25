package com.chat.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = true)
    private String phoneNumber;

    @Column(unique = true, nullable = true)
    private String email;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String avatar;

    private boolean searchable = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinTable(
        name = "user_contacts",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    private Set<User> contacts = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void validateContactInfo() {
        if ((phoneNumber == null || phoneNumber.isEmpty()) &&
            (email == null || email.isEmpty())) {
            throw new IllegalArgumentException("Either phoneNumber or email must be provided, but not both null");
        }
    }
}
