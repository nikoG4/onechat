package com.chat.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chat.models.User;
import com.chat.models.response.UserResponse;
import com.chat.services.UserService;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/contacts/{id}")
    public ResponseEntity<UserResponse> getUserWithContacts(@PathVariable Long id) {

        User user = userService.findById(id);
        UserResponse userResponse = new UserResponse(user);

        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<User> getUserByPhoneNumber(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(userService.findByPhoneNumber(phoneNumber).orElse(null));
    }

    @GetMapping("search/{userId}")
    public List<User> getUsers(@PathVariable Long userId, @RequestParam String param) {
        return userService.findByNameOrPhoneNumber(param, userId);
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@RequestBody User user) {

        User existingUser = userService.findByPhoneNumber(user.getPhoneNumber()).orElse(null);
        if(existingUser != null) {

            existingUser.setName(user.getName());
            existingUser.setAvatar(user.getAvatar());
            existingUser.setSearchable(user.isSearchable());
            
            userService.createUser(existingUser);

            UserResponse userResponse = new UserResponse(existingUser);
            System.out.println("Existing user: " + userResponse);

            return ResponseEntity.ok(userResponse);
        }

        // Usuario no existe -> crear.
        User savedUser = userService.createUser(user);
        return ResponseEntity.ok(new UserResponse(savedUser));
    }
}
