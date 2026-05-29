package com.example.mailsystem.repository;

import com.example.mailsystem.domain.DeliveryStatus;
import com.example.mailsystem.domain.EmailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {

    Optional<EmailDelivery> findByTrackingId(String trackingId);

    Optional<EmailDelivery> findByMessageId(String messageId);

    /** Most recent delivery to a given address, used to attach bounces/replies. */
    Optional<EmailDelivery> findFirstByRecipientEmailIgnoreCaseOrderBySentAtDesc(String recipientEmail);

    List<EmailDelivery> findByStatusAndSentAtBefore(DeliveryStatus status, Instant before);
}
