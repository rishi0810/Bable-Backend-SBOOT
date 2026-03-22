package com.bable.b_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bable.b_backend.mappers.JWTBody;

// A helper auth function used to extract the current user authenticated in a wide context
@Component
public class AuthContext {
    public JWTBody getCurrentUser () {
        // An object for the current security context -> extraction of authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JWTBody currentUser)) {
            throw new RuntimeException("User Not Authenticated");
        }
        // return current logged in user
        return currentUser;
    }

}
