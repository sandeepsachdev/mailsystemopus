package com.example.mailsystem.dto;

import com.example.mailsystem.domain.EmailDelivery;

import java.time.Instant;
import java.util.List;

/**
 * Read-only JSON projection of a single recipient's delivery, including the timestamps for each
 * tracked event and any replies.
 */
public record DeliveryView(
        Long id,
        String recipientEmail,
        String status,
        Instant sentAt,
        Instant receivedAt,
        Instant readAt,
        int openCount,
        Instant bouncedAt,
        String bounceReason,
        Instant repliedAt,
        List<ReplyView> replies) {

    public static DeliveryView from(EmailDelivery d) {
        List<ReplyView> replies = d.getReplies().stream().map(ReplyView::from).toList();
        return new DeliveryView(
                d.getId(),
                d.getRecipientEmail(),
                d.getStatus().name(),
                d.getSentAt(),
                d.getDeliveredAt(),
                d.getFirstOpenedAt(),
                d.getOpenCount(),
                d.getBouncedAt(),
                d.getBounceReason(),
                d.getRepliedAt(),
                replies);
    }
}
