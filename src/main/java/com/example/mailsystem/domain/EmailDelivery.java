package com.example.mailsystem.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the fate of a campaign email sent to a single recipient address.
 *
 * <p>Each delivery carries a unique {@code trackingId} that is embedded both as a 1x1 tracking
 * pixel URL (to detect opens/reads) and as the {@code X-Mailtrack-Id} header on the outgoing
 * message (which survives in the original message returned inside bounce DSNs, allowing bounces
 * to be matched back to the exact delivery).
 */
@Entity
@Table(name = "email_delivery", indexes = {
        @Index(name = "idx_delivery_tracking", columnList = "trackingId"),
        @Index(name = "idx_delivery_recipient", columnList = "recipientEmail")
})
public class EmailDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(nullable = false)
    private String recipientEmail;

    /** Opaque per-delivery token used by the tracking pixel and the X-Mailtrack-Id header. */
    @Column(nullable = false, unique = true)
    private String trackingId;

    /** Message-ID we set on the outgoing message (best-effort matching aid for replies). */
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.SENT;

    private Instant sentAt;
    private Instant deliveredAt;   // "received"
    private Instant firstOpenedAt; // "read"
    private Instant lastOpenedAt;
    private int openCount;
    private Instant bouncedAt;
    @Column(length = 2000)
    private String bounceReason;
    private Instant repliedAt;

    @Column(length = 2000)
    private String errorMessage;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("receivedAt ASC")
    private List<EmailReply> replies = new ArrayList<>();

    /** A delivery counts as "received" once delivered, opened, or replied to. */
    public boolean isReceived() {
        return deliveredAt != null || firstOpenedAt != null || repliedAt != null;
    }

    public void addReply(EmailReply reply) {
        reply.setDelivery(this);
        this.replies.add(reply);
    }

    /** Records an open from the tracking pixel; never downgrades a BOUNCED/REPLIED status. */
    public void registerOpen(Instant when) {
        this.openCount++;
        this.lastOpenedAt = when;
        if (this.firstOpenedAt == null) {
            this.firstOpenedAt = when;
        }
        if (this.deliveredAt == null) {
            this.deliveredAt = when; // an open proves the message was received
        }
        if (this.status == DeliveryStatus.SENT || this.status == DeliveryStatus.RECEIVED) {
            this.status = DeliveryStatus.READ;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getFirstOpenedAt() {
        return firstOpenedAt;
    }

    public void setFirstOpenedAt(Instant firstOpenedAt) {
        this.firstOpenedAt = firstOpenedAt;
    }

    public Instant getLastOpenedAt() {
        return lastOpenedAt;
    }

    public void setLastOpenedAt(Instant lastOpenedAt) {
        this.lastOpenedAt = lastOpenedAt;
    }

    public int getOpenCount() {
        return openCount;
    }

    public void setOpenCount(int openCount) {
        this.openCount = openCount;
    }

    public Instant getBouncedAt() {
        return bouncedAt;
    }

    public void setBouncedAt(Instant bouncedAt) {
        this.bouncedAt = bouncedAt;
    }

    public String getBounceReason() {
        return bounceReason;
    }

    public void setBounceReason(String bounceReason) {
        this.bounceReason = bounceReason;
    }

    public Instant getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(Instant repliedAt) {
        this.repliedAt = repliedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<EmailReply> getReplies() {
        return replies;
    }

    public void setReplies(List<EmailReply> replies) {
        this.replies = replies;
    }
}
