package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssistanceAssignRequest {

    @NotBlank(message = "Agent ID is required")
    private String agentId;
}
