package com.example.mailsystem.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A single send: one subject + body sent to one or more recipients. Each recipient gets its
 * own {@link EmailDelivery} so that received/read/bounced/replied can be tracked per address.
 */
@Entity
@Table(name = "campaign")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String body;

    /** Name of the mailing list this campaign was sent to (for display), if any. */
    private String mailingListName;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant sentAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recipientEmail ASC")
    private List<EmailDelivery> deliveries = new ArrayList<>();

    public void addDelivery(EmailDelivery delivery) {
        delivery.setCampaign(this);
        this.deliveries.add(delivery);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMailingListName() {
        return mailingListName;
    }

    public void setMailingListName(String mailingListName) {
        this.mailingListName = mailingListName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public List<EmailDelivery> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<EmailDelivery> deliveries) {
        this.deliveries = deliveries;
    }

    // --- Aggregate counts used by the dashboard -----------------------------

    public long getTotalCount() {
        return deliveries.size();
    }

    public long getReceivedCount() {
        return deliveries.stream().filter(EmailDelivery::isReceived).count();
    }

    public long getReadCount() {
        return deliveries.stream().filter(d -> d.getFirstOpenedAt() != null).count();
    }

    public long getBouncedCount() {
        return deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.BOUNCED).count();
    }

    public long getRepliedCount() {
        return deliveries.stream().filter(d -> d.getRepliedAt() != null).count();
    }
}
