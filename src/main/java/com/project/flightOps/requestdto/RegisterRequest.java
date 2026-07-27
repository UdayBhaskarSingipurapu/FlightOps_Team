package com.project.flightOps.requestdto;

import com.project.flightOps.enums.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    private String name;
    private String email;
    private String password;
    private String airportId;

    @NotNull
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be 10 digits")
    private String phone;
    private Role role;
}
