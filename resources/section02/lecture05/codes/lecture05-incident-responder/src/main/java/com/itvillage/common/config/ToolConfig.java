package com.itvillage.common.config;

import com.itvillage.incident.adapter.out.ai.IncidentTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    @Bean
    public IncidentTools incidentTools() {
        return new IncidentTools();
    }
}
