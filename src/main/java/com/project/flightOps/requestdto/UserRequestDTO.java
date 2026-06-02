package com.project.flightOps.requestdto;


import com.project.flightOps.enums.Roles;

public class UserRequestDTO {
    private String email;
    private String fullName;
    private String password;
    private String address;
    private String phoneNo;
    private Roles role;


    public UserRequestDTO(String email, String fullName, String password, String address, String phoneNo, Roles role) {
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.address = address;
        this.phoneNo = phoneNo;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
