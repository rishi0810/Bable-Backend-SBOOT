package com.bable.b_backend.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bable.b_backend.utils.Constants;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JWTFilter filter;

    // ALl publically accessible routes
     private static final String [] publicRoutes = Constants.publicRoutes; 
    // Security filter Bean for all routes
    @Bean
    public SecurityFilterChain filterChain (HttpSecurity http) throws Exception {
       // HTTP guidelines
       http
            // CSRF -> Not needed as JWT enables default behaviour
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            
            // Define which routes to protect and which to permit universally
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(publicRoutes).permitAll() 
                .anyRequest().authenticated()             
            )
            
            // No Session on backend side -> simple stateless JWT auth
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Application of JWT filter in the filterchain
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allowed origin localhost & frontend
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "https://bable.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // Register CORS setup to the configuration
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
