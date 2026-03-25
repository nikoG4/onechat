package com.chat.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.models.User;
import com.chat.models.response.UserResponse;
import com.chat.repositories.UserRepository;

@Service
public class ContactService {
    
    @Autowired
    private UserRepository userRepository;

    public User addContact(Long userId, Long contactId) {

        // User user = userRepository.findById(userId).orElse(null);
        // User contact = userRepository.findById(contactId).orElse(null);
        // if (user != null && contact != null) {
        //     user.addContact(contact);
        //     userRepository.save(user);
        //     return contact;
        // }
        return null;

    }


    @Transactional
    public Set<User> getContactsHql(Long userId) {

        Set<User> contacts = userRepository.findContacts(userId);

        contacts.forEach(contact -> System.out.println(contact.getName()));

        return contacts;
    }


    public List<User> searchContacts(Long userId, String searchTerm) {
        return userRepository.findContactsBySearchTerm(userId, searchTerm);
    }
}
