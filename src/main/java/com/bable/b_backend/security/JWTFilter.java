package com.bable.b_backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bable.b_backend.mappers.JWTBody;
import com.bable.b_backend.utils.Constants;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;



@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final List<RequestMatcher> PUBLIC_ROUTE_MATCHERS = buildPublicRouteMatchers();

    // Auto Injection of JWT token logic configuration
    @Autowired
    private JWTConfig jwtConfig;


    // Building list of routes matching the public routes
    private static List<RequestMatcher> buildPublicRouteMatchers() {
        List<RequestMatcher> matchers = new ArrayList<>();
        for (String route : Constants.publicRoutes) {
            matchers.add(PathPatternRequestMatcher.pathPattern(route));
        }
        return matchers;
    }

    // Ignore filtering -> spring native + JWT validation
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_ROUTE_MATCHERS
            .stream()
            .anyMatch(matcher -> matcher.matches(request));
    }

    // Function defining filtering logic for middleware
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String jwt = null;
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Extraction of JWT token from Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }

        // If a user is not signed in -> ignore and proceed
        if (jwt == null) {
            chain.doFilter(request, response);
            return;
        }
        
        // If a user is signed in
        try {
            // Extract the current user + remaining time via JWTBody
            JWTBody user = jwtConfig.getCurrentUser(jwt);
            long remainingTime = jwtConfig.remainingTime(jwt);
            
            // If there is less than 60 seconds remaining for the current JWT token
            if (remainingTime <= Constants.REFRESH_TIME) {
                String newToken = jwtConfig.generateJWTToken(user);
                response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newToken);
            } 

            // Create a new Username-Password based user for the current context
            UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            // The details of the current user is created for global context
            token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(token);
        } catch (Exception e) {

            // JWT invalid or not applicable -> Clear Auth Context + Send invalid response

            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Invalid or expired JWT");
            return;
        }
        // Filter end
        chain.doFilter(request, response);
    }
}
