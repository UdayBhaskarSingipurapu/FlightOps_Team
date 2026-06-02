package com.project.flightOps.responsedto;


import com.project.flightOps.entity.User;

public class UserAuthResponseDTO {
    private String token;
    private String role;
    private User user; // Add this field

    // Constructors
    public UserAuthResponseDTO(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public UserAuthResponseDTO(String token, String role, User user) {
        this.token = token;
        this.role = role;
        this.user = user;
    }

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
