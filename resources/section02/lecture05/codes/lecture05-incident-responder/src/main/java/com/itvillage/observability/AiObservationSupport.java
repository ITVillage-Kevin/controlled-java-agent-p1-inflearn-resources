package com.itvillage.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiObservationSupport {

    private static final Logger log = LoggerFactory.getLogger(AiObservationSupport.class);

    public void mark(String stage, String detail) {
        log.info("[ai-stage] stage={}, detail={}", stage, detail);
    }
}
