package com.omnicall;

import com.omnicall.config.OmniCallProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OmniCallProperties.class)
public class OmniCallApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmniCallApplication.class, args);
    }
}
