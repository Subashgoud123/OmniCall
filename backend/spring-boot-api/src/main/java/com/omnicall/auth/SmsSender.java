package com.omnicall.auth;

public interface SmsSender {

    void send(String phoneNumber, String code);
}
