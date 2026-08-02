package com.mindhaven.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * 内部工具使用的 Jackson2 ObjectMapper（仅用于 List/Map 等简单 JSON）。
     * HTTP 消息编解码由 Spring Boot 4 默认的 Jackson3 (tools.jackson) 负责。
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
