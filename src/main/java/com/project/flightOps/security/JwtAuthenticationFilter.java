package com.project.flightOps.security;

import com.project.flightOps.service.UserServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserServiceImpl userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserServiceImpl userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return; // Stop processing the filter chain
        }
        // --- END: ADD THIS CODE BLOCK ---


        final String requestURI = request.getRequestURI();
        System.out.println("JWT Filter - Request URI: " + requestURI);

        // Skip JWT processing for authentication endpoints
        if (isAuthEndpoint(requestURI)) {
            System.out.println("JWT Filter - Skipping authentication endpoint: " + requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        System.out.println("JWT Filter - Auth header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("JWT Filter - No valid auth header, continuing without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        System.out.println("JWT Filter - Extracted JWT: " + jwt);

        try {
            userEmail = jwtUtil.extractUsername(jwt);
            System.out.println("JWT Filter - Extracted email: " + userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(userEmail);
                System.out.println("JWT Filter - User details loaded: " + userDetails.getUsername());

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    System.out.println("JWT Filter - Token is valid");
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("JWT Filter - Authentication set in security context");
                } else {
                    System.out.println("JWT Filter - Token validation failed");
                }
            }
        } catch (Exception e) {
            System.out.println("JWT Filter - Exception during processing: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception, just continue the filter chain
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.equals("/users/login") ||
                uri.equals("/users/register") ||
                uri.startsWith("/users/login") ||
                uri.startsWith("/users/register");
    }
}
