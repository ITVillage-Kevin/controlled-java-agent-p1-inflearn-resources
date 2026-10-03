package com.itvillage.incident.port.in;

import com.itvillage.incident.dto.IncidentRequest;
import com.itvillage.incident.dto.IncidentResponse;

public interface RespondToIncidentUseCase {
    IncidentResponse respond(IncidentRequest request);
}
