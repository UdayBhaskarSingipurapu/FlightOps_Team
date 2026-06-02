package com.project.flightOps.entity;

import com.project.flightOps.enums.Roles;
import jakarta.persistence.*;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private Long userId;
    private String fullName;
    private String email;

    @Enumerated(EnumType.STRING)
    private Roles role;

    private String password;
    private String phoneNo;
    private String address;

    public User(){}
    public User(Long userId, String name, String email, Roles role, String password, String phoneNo, String address) {
        this.userId = userId;
        this.fullName = name;
        this.email = email;
        this.role = role;
        this.password = password;
        this.phoneNo = phoneNo;
        this.address = address;
    }

    public User(Long userId, String fullName, String email, Roles role, String phoneNo, String address) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.phoneNo = phoneNo;
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    public Long getUserID() {
        return userId;
    }

    public void setUserID(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", password='" + password + '\'' +
                ", phoneNo=" + phoneNo +
                ", address='" + address + '\'' +
                '}';
    }
}
