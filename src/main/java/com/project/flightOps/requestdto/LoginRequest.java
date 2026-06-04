package com.project.flightOps.requestdto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
