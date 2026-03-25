package com.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.web.SecurityFilterChain;

//@Configuration
public class SecurityConfig {

    // @Bean
    // SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //     http
    //         .authorizeHttpRequests(auth -> auth
    //             // Permitir acceso público a las páginas de login, registro y error
    //             // Permitir el acceso a cualquier otra solicitud para usuarios autenticados
    //             .anyRequest().permitAll()
                
    //         )
    //         // Aquí podrías configurar OAuth2 o cualquier otro método de autenticación que necesites
    //         /* .oauth2Login(oauth2 -> oauth2
    //             .userInfoEndpoint(userInfo -> userInfo
    //                 .userService(customOAuth2UserService)
    //             )
    //             .defaultSuccessUrl("/auth/success", true)  // Redirige después de loguear
    //             .failureUrl("/login?error=true")  // Redirige a "/login" si hay error
    //         ) */;
        
    //     return http.build();
    // }
}
