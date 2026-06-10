package com.project.flightOps.responsedto;

import com.project.flightOps.enums.Role;
import com.project.flightOps.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String userId;
    private String name;
    private String email;
    private Role role;
    private String phone;
    private String airportId;
    private UserStatus status;
}
