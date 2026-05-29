package com.example.mailsystem.dto;

import com.example.mailsystem.domain.Campaign;

import java.time.Instant;
import java.util.List;

/**
 * Read-only JSON projection of a campaign with aggregate counts and per-recipient deliveries.
 */
public record CampaignView(
        Long id,
        String subject,
        String mailingListName,
        Instant sentAt,
        long total,
        long received,
        long read,
        long bounced,
        long replied,
        List<DeliveryView> deliveries) {

    public static CampaignView from(Campaign c) {
        return new CampaignView(
                c.getId(),
                c.getSubject(),
                c.getMailingListName(),
                c.getSentAt(),
                c.getTotalCount(),
                c.getReceivedCount(),
                c.getReadCount(),
                c.getBouncedCount(),
                c.getRepliedCount(),
                c.getDeliveries().stream().map(DeliveryView::from).toList());
    }

    /** Summary form without the per-recipient list, for the campaigns index. */
    public static CampaignView summary(Campaign c) {
        return new CampaignView(
                c.getId(),
                c.getSubject(),
                c.getMailingListName(),
                c.getSentAt(),
                c.getTotalCount(),
                c.getReceivedCount(),
                c.getReadCount(),
                c.getBouncedCount(),
                c.getRepliedCount(),
                List.of());
    }
}
