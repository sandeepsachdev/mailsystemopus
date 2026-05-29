package com.example.mailsystem.service;

import com.example.mailsystem.config.MailSystemProperties;
import com.example.mailsystem.domain.DeliveryStatus;
import com.example.mailsystem.domain.EmailDelivery;
import com.example.mailsystem.repository.EmailDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Records email opens reported by the tracking pixel and infers "received" status for deliveries
 * that were sent but never bounced.
 */
@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final EmailDeliveryRepository deliveryRepository;
    private final MailSystemProperties properties;

    public TrackingService(EmailDeliveryRepository deliveryRepository, MailSystemProperties properties) {
        this.deliveryRepository = deliveryRepository;
        this.properties = properties;
    }

    /** Called when the tracking pixel for a delivery is loaded by the recipient's mail client. */
    @Transactional
    public void recordOpen(String trackingId) {
        deliveryRepository.findByTrackingId(trackingId).ifPresent(delivery -> {
            delivery.registerOpen(Instant.now());
            deliveryRepository.save(delivery);
            log.info("Recorded open #{} for {} (trackingId={})",
                    delivery.getOpenCount(), delivery.getRecipientEmail(), trackingId);
        });
    }

    /**
     * Promotes deliveries that have been SENT longer than the configured grace period (and have
     * not bounced) to RECEIVED. SMTP acceptance plus the absence of a bounce is treated as
     * successful delivery.
     */
    @Scheduled(fixedDelayString = "${mailsystem.imap.poll-interval-ms:60000}")
    @Transactional
    public void markDeliveredAfterGrace() {
        Instant cutoff = Instant.now().minus(properties.getDeliveredGraceMinutes(), ChronoUnit.MINUTES);
        List<EmailDelivery> candidates =
                deliveryRepository.findByStatusAndSentAtBefore(DeliveryStatus.SENT, cutoff);
        for (EmailDelivery delivery : candidates) {
            delivery.setStatus(DeliveryStatus.RECEIVED);
            if (delivery.getDeliveredAt() == null) {
                delivery.setDeliveredAt(Instant.now());
            }
            deliveryRepository.save(delivery);
        }
        if (!candidates.isEmpty()) {
            log.info("Marked {} delivery(ies) as RECEIVED after grace period", candidates.size());
        }
    }
}
