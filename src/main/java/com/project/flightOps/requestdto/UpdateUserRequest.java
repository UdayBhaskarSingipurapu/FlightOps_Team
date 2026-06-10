package com.project.flightOps.requestdto;

import com.project.flightOps.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    private String phone;

    @NotBlank(message = "Airport ID is required")
    private String airportId;
}
