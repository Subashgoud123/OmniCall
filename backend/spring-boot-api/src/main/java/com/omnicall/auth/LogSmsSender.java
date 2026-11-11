package com.omnicall.auth;

import com.omnicall.config.OmniCallProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LogSmsSender.class);
    private final OmniCallProperties properties;

    public LogSmsSender(OmniCallProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(String phoneNumber, String code) {
        if (!"log".equalsIgnoreCase(properties.getSms().getProvider())) {
            return;
        }
        log.info("Development SMS for {}: your OmniCall code is {}", phoneNumber, code);
    }
}
