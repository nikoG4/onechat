package com.chat.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.chat.models.User;
import com.chat.services.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class FileController {
    private final Path uploadDir = Paths.get("uploads");

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
   
        try {
            Path file = uploadDir.resolve(filename);

            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
   
                String contentType = Files.probeContentType(file);

                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType)) 
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

}
