package com.chat.models.response;

import java.util.Set;
import java.util.stream.Collectors;

import com.chat.models.User;
import com.chat.models.dto.ContactDTO;

import lombok.Data;


@Data

public class UserResponse {
    private Long id;
    private String name;
    private String avatar;
    private String email;
    private String phoneNumber;
    private boolean searchable;
    private Set<ContactDTO> contacts;


    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.avatar = user.getAvatar();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.searchable = user.isSearchable();

        this.contacts = user.getContacts().stream()
        .map(contact -> new ContactDTO(contact.getId(), contact.getName(), contact.getAvatar()))
        .collect(Collectors.toSet());
    }


}
