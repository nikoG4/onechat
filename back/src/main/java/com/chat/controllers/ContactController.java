package com.chat.controllers;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.models.User;

import com.chat.services.ContactService;

@RestController
@RequestMapping("api/contacts")
public class ContactController {
    

    @Autowired
    private ContactService contactService;

    @GetMapping("/{userId}")
    public ResponseEntity<Set<User>> getContacts(@PathVariable Long userId) {

        Set<User> contacts = contactService.getContactsHql(userId);

        return ResponseEntity.ok(contacts);
    }



}
