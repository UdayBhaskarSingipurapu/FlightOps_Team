package com.project.flightOps.requestdto;

import com.project.flightOps.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;
}
