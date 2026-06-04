package com.project.flightOps.requestdto;

import com.project.flightOps.enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {

    private String name;
    private String email;
    private String password;
    private String airportId;
    private String phone;
    private Role role;
}
