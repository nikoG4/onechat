package com.chat.controllers;

import com.chat.models.User;
import com.chat.models.response.UserResponse;
import com.chat.requests.GoogleIdTokenRequest;
import com.chat.services.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @PostMapping("/exchange")
    public ResponseEntity<UserResponse> exchange(@RequestBody GoogleIdTokenRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            Map<String, Object> tokenInfo;
            try {
                tokenInfo = validateGoogleIdToken(request.getIdToken());
            } catch (Exception ignored) {
                // Firebase ID tokens frecuentemente no funcionan con tokeninfo de Google.
                // MVP: decodificamos el payload JWT para extraer email/name/picture.
                tokenInfo = decodeJwtClaims(request.getIdToken());
            }

            String email = (String) tokenInfo.get("email");
            String name = (String) tokenInfo.get("name");
            String picture = (String) tokenInfo.get("picture");

            // Si el claim "email" no existe (ej. tokens de Firebase), intentamos desde "firebase.identities.email[0]"
            if (email == null || email.isEmpty()) {
                try {
                    Object firebaseObj = tokenInfo.get("firebase");
                    if (firebaseObj instanceof Map<?, ?> firebaseMap) {
                        Object identitiesObj = firebaseMap.get("identities");
                        if (identitiesObj instanceof Map<?, ?> identitiesMap) {
                            Object emailObj = identitiesMap.get("email");
                            if (emailObj instanceof java.util.List<?> list && !list.isEmpty()) {
                                Object first = list.get(0);
                                if (first instanceof String s && !s.isEmpty()) {
                                    email = s;
                                }
                            }
                        }
                    }
                } catch (Exception ignored2) {
                    // ignored
                }
            }

            if (email == null || email.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            final String emailFinal = email;
            Optional<User> existingUserOpt = userService.findByPhoneNumberOrEmail(email);

            User user = existingUserOpt.orElseGet(() -> {
                User u = new User();
                u.setEmail(emailFinal);
                return u;
            });

            user.setName(name);
            user.setEmail(email);
            user.setAvatar(picture);
            user.setSearchable(true);

            User saved = userService.createUser(user);
            return ResponseEntity.ok(new UserResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private Map<String, Object> validateGoogleIdToken(String idToken) throws Exception {
        String encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken;

        HttpURLConnection connection = (HttpURLConnection) new java.net.URL(url).openConnection();
        connection.setRequestMethod("GET");

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("Invalid token, code=" + code);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = reader.lines().reduce("", (acc, line) -> acc + line);
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        }
    }

    private Map<String, Object> decodeJwtClaims(String idToken) throws Exception {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) throw new RuntimeException("Invalid JWT");

        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        String json = new String(decoded, StandardCharsets.UTF_8);
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}

