package com.report.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class Config {
    @Bean
    ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}
