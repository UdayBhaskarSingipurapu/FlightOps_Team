package com.project.flightOps.responsedto;


import com.project.flightOps.enums.Roles;

public class UserResponseDTO {

    private Long userId;
    private String email;
    private String fullName;
    private String address;
    private String phoneNo;
    private Roles role;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public UserResponseDTO(Long userId, String email, String fullName, Roles role, String phoneNo, String address) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.address=address;
        this.phoneNo=phoneNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }


}
