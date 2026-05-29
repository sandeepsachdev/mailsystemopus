package com.example.mailsystem.dto;

import com.example.mailsystem.domain.EmailReply;

import java.time.Instant;

public record ReplyView(
        String fromAddress,
        String subject,
        String snippet,
        Instant receivedAt) {

    public static ReplyView from(EmailReply r) {
        return new ReplyView(r.getFromAddress(), r.getSubject(), r.getSnippet(), r.getReceivedAt());
    }
}
