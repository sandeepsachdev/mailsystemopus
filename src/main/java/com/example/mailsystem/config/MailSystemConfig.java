package com.example.mailsystem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link MailSystemProperties}. The {@link org.springframework.mail.javamail.JavaMailSender}
 * used for sending is auto-configured by Spring Boot from the {@code spring.mail.*} properties.
 */
@Configuration
@EnableConfigurationProperties(MailSystemProperties.class)
public class MailSystemConfig {
}
