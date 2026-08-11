package com.itvillage.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiObservationSupport {

    private static final Logger log = LoggerFactory.getLogger(AiObservationSupport.class);

    public void mark(String stage, String incidentId) {
        log.info("AI observation marker. stage={}, incidentId={}", stage, incidentId);
    }
}
