package com.project.flightOps.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcryptEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        System.out.println("=== CustomPasswordEncoder Debug ===");
        System.out.println("Raw password: " + rawPassword.toString());
        System.out.println("Encoded password: " + encodedPassword);
        System.out.println("Encoded password length: " + encodedPassword.length());

        // First try BCrypt matching (for properly encoded passwords)
        if (bcryptEncoder.matches(rawPassword, encodedPassword)) {
            System.out.println("BCrypt match successful!");
            return true;
        }

        // If BCrypt fails, try plain text matching (for existing users with plain text passwords)
        if (rawPassword.toString().equals(encodedPassword)) {
            System.out.println("Plain text match successful!");
            return true;
        }

        // If still no match, try checking if the encoded password is actually plain text
        // that was stored without encoding
        if (encodedPassword != null && encodedPassword.length() < 20) {
            // Likely plain text password, try direct comparison
            boolean match = rawPassword.toString().equals(encodedPassword);
            System.out.println("Plain text comparison result: " + match);
            return match;
        }

        System.out.println("No match found");
        return false;
    }
}
