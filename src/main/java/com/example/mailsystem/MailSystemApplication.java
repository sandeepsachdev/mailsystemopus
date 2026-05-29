package com.example.mailsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Gmail-backed mailing system.
 *
 * <p>The application can send campaigns to mailing lists via Gmail SMTP and then read the
 * Gmail mailbox back over IMAP to detect bounces and replies, while a tracking pixel records
 * opens (reads). Scheduling is enabled so the mailbox is polled periodically.
 */
@SpringBootApplication
@EnableScheduling
public class MailSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailSystemApplication.class, args);
    }
}
