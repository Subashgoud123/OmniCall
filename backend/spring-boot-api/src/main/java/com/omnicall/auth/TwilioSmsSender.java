package com.omnicall.auth;

import com.omnicall.common.ApiException;
import com.omnicall.config.OmniCallProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TwilioSmsSender implements SmsSender {

    private final OmniCallProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TwilioSmsSender(OmniCallProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(String phoneNumber, String code) {
        if (!"twilio".equalsIgnoreCase(properties.getSms().getProvider())) {
            return;
        }
        OmniCallProperties.Sms sms = properties.getSms();
        if (sms.getTwilioAccountSid().isBlank() || sms.getTwilioAuthToken().isBlank() || sms.getTwilioFromNumber().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_NOT_CONFIGURED", "SMS delivery is not configured.");
        }
        String body = "To=" + encode(phoneNumber)
                + "&From=" + encode(sms.getTwilioFromNumber())
                + "&Body=" + encode("Your OmniCall code is " + code);
        String basic = Base64.getEncoder().encodeToString(
                (sms.getTwilioAccountSid() + ":" + sms.getTwilioAuthToken()).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sms.getTwilioAccountSid() + "/Messages.json"))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "SMS_FAILED", "The verification code could not be sent.");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SMS_FAILED", "The verification code could not be sent.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
