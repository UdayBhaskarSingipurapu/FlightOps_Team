package com.project.flightOps.util;

public class CustomUserPrinciple {
    private Long userId;
    private String email;

    public CustomUserPrinciple(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}