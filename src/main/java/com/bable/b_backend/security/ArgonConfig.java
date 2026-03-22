package com.bable.b_backend.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;



public class ArgonConfig {

    // Parameterized Encoder | Validator following Argon2PasswordEncoder
    private static final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 4, 65536, 3);

    // Hash Password -> From raw password from user in UserBody DTO
    public static String hashPassword (String password){
        try {
            String hashedPassword = encoder.encode(password);
            return hashedPassword;
        } catch (Exception e) {
            return null;
        }
    }

    // Return Boolean for comparison of DTO and DB password
    public static boolean comparePassword (String rawPassword, String hashedPassword){
            return encoder.matches(rawPassword, hashedPassword);
    }
}
