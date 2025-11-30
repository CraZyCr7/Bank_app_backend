package com.bankapp.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Async
    public void sendEmailAsync(String to, String subject, String body) {
        // For now: simple log. Replace with real email sender (SMTP / AWS SES) later.
        System.out.println("[EMAIL] To: " + to + " | Subject: " + subject + " | Body: " + body);
    }
}
