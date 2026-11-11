package com.omnicall.common;

import java.util.regex.Pattern;

public final class PhoneNumbers {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "PHONE_INVALID", "Enter a phone number.");
        }
        String compact = raw.trim().replace(" ", "").replace("-", "");
        if (!E164.matcher(compact).matches()) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "PHONE_INVALID",
                    "Enter the phone number in international format, for example +9198XXXXXXXX.");
        }
        return compact;
    }
}
