package com.itvillage.incident.adapter.in;

import com.itvillage.incident.dto.IncidentRequest;
import com.itvillage.incident.dto.IncidentResponse;
import com.itvillage.incident.port.in.RespondToIncidentUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentResponderController {

    private final RespondToIncidentUseCase respondToIncidentUseCase;

    public IncidentResponderController(RespondToIncidentUseCase respondToIncidentUseCase) {
        this.respondToIncidentUseCase = respondToIncidentUseCase;
    }

    @PostMapping("/respond")
    public IncidentResponse respond(@Valid @RequestBody IncidentRequest request) {
        return respondToIncidentUseCase.respond(request);
    }
}