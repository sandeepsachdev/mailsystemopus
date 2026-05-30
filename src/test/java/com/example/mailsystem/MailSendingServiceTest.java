package com.example.mailsystem;

import com.example.mailsystem.config.MailSystemProperties;
import com.example.mailsystem.service.MailSendingService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the pre-send credential check produces an actionable error rather than letting every
 * delivery fail with a terse "Authentication failed".
 */
class MailSendingServiceTest {

    @Test
    void sendWithoutCredentialsFailsWithActionableMessage() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl(); // no username/password set
        MailSendingService service =
                new MailSendingService(sender, null, new MailSystemProperties());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.sendCampaign("Subject", "Body", null, List.of("a@example.com")));

        assertTrue(ex.getMessage().contains("GMAIL_APP_PASSWORD"),
                "message should point the user at the app-password env var");
    }
}
