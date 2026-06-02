package com.project.flightOps.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SecurityUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static boolean passwordMatches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    public static Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrinciple) {
            return ((CustomUserPrinciple) principal).getUserId();
        }

        return null;
    }
    public static String getCurrentUserEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrinciple) {
            return ((CustomUserPrinciple) principal).getEmail();
        }
        return null;
    }
}
