package com.chat.services;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.chat.models.User;
import com.chat.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createUser(User user) {
        // Si el avatar viene en Base64 (desde RegisterScreen), lo guardamos en `uploads/`.
        // Si viene como URL (desde Google), lo guardamos tal cual.
        if (user.getAvatar() != null) {
            String avatar = user.getAvatar();
            if (looksLikeBase64Image(avatar)) {
                String imageName = user.getPhoneNumber() != null ? user.getPhoneNumber() : user.getEmail();
                user.setAvatar(uploadImage(normalizeBase64Image(avatar), imageName + ".jpg"));
            }
        }

        User savedUser = userRepository.save(user);
        savedUser.setContacts(userRepository.findContacts(savedUser.getId()));

        return savedUser;
    }

    private boolean looksLikeBase64Image(String value) {
        String v = value.trim();
        // Covers common cases:
        // - "data:image/png;base64,AAAA..."
        // - "AAAA..." (raw base64)
        return v.startsWith("data:") && v.contains(";base64,") || v.matches("^[A-Za-z0-9+/=\\r\\n]+$");
    }

    private String normalizeBase64Image(String value) {
        String v = value.trim();
        if (v.startsWith("data:") && v.contains(";base64,")) {
            return v.substring(v.indexOf(";base64,") + ";base64,".length());
        }
        return v;
    }

    public Optional<User> findByName(String username) {
        return userRepository.findByName(username);
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> findByPhoneNumberOrEmail(String q) {
        return userRepository.findByPhoneNumberOrEmail(q, q);
    }

    /**
     * Finds all users with a name or phone number that matches the given query string,
     * excluding the user with the given id.
     *
     * @param query the query string to search for.
     * @param userId the id of the user to exclude from the search.
     * @return a list of users that match the given query string.
     */
    public List<User> findByNameOrPhoneNumber(String query, Long userId) {
        return userRepository
            .findBySearchableAndQuery(query,userId);
    }

    @Transactional
    public User findById(Long id) {

        User user = userRepository.findById(id).orElse(null);
        
        user.getContacts().forEach(contact -> Hibernate.initialize(contact.getContacts()));

        if(user.getContacts().size() > 0)
            System.out.println("hay");
        else
            System.out.println("no hay");

        return user;
    }

    public String uploadImage(String Base64Image, String fileName) {
        try {
            // Decodificar la imagen en Base64
            byte[] imageBytes = Base64.getDecoder().decode(Base64Image);

            // Especificar el directorio de almacenamiento
            String uploadDir = "uploads/";
            File directory = new File(uploadDir);
            
            if (!directory.exists()) {
                directory.mkdirs(); // Crear el directorio si no existe
            }

            // Crear el archivo con el nombre recibido
            String filePath = uploadDir + fileName;
            try (OutputStream outputStream = new FileOutputStream(filePath)) {
                outputStream.write(imageBytes); // Guardar la imagen
            }

            // Construir la URL para compartir
            return  filePath;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
