package com.example.jira.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"
    );

    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        if (!STRONG_PASSWORD.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and include uppercase, lowercase, digit, and special character.");
        }
    }
}
