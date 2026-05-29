package com.example.mailsystem.service;

import com.example.mailsystem.config.MailSystemProperties;
import com.example.mailsystem.domain.DeliveryStatus;
import com.example.mailsystem.domain.EmailDelivery;
import com.example.mailsystem.domain.EmailReply;
import com.example.mailsystem.repository.EmailDeliveryRepository;
import com.example.mailsystem.repository.EmailReplyRepository;
import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Connects to the Gmail mailbox over IMAP and reads messages back to detect:
 * <ul>
 *   <li><b>Bounces</b> — Delivery Status Notifications from the mailer-daemon. The failed
 *       recipient and the original {@code X-Mailtrack-Id} header (echoed inside the DSN) are
 *       parsed out and matched to the originating {@link EmailDelivery}.</li>
 *   <li><b>Replies</b> — messages whose {@code In-Reply-To}/{@code References} reference one of
 *       our sent message-ids, or whose sender + normalized subject match a delivery.</li>
 * </ul>
 * Runs on a fixed schedule and can also be triggered manually from the dashboard.
 */
@Service
public class MailboxScanService {

    private static final Logger log = LoggerFactory.getLogger(MailboxScanService.class);

    private static final Pattern FINAL_RECIPIENT =
            Pattern.compile("(?im)^(?:Final|Original)-Recipient:\\s*(?:rfc822;)?\\s*<?([^>\\s]+@[^>\\s]+)>?");
    private static final Pattern MAILTRACK_ID =
            Pattern.compile("(?im)^X-Mailtrack-Id:\\s*([0-9a-fA-F]+)");
    private static final Pattern DIAGNOSTIC =
            Pattern.compile("(?im)^(?:Diagnostic-Code|Status):\\s*(.+)$");
    private static final Pattern MAILTRACK_MSGID =
            Pattern.compile("<([0-9a-fA-F]+)@mailsystem\\.local>");

    private final MailSystemProperties properties;
    private final EmailDeliveryRepository deliveryRepository;
    private final EmailReplyRepository replyRepository;
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    public MailboxScanService(MailSystemProperties properties,
                              EmailDeliveryRepository deliveryRepository,
                              EmailReplyRepository replyRepository) {
        this.properties = properties;
        this.deliveryRepository = deliveryRepository;
        this.replyRepository = replyRepository;
    }

    @Scheduled(fixedDelayString = "${mailsystem.imap.poll-interval-ms:60000}",
            initialDelayString = "${mailsystem.imap.poll-interval-ms:60000}")
    public void scheduledScan() {
        if (!properties.getImap().isEnabled()) {
            return;
        }
        try {
            scan();
        } catch (Exception ex) {
            log.warn("Scheduled mailbox scan failed: {}", ex.getMessage());
        }
    }

    /**
     * Reads recent mailbox messages and updates deliveries with any bounces/replies found.
     *
     * @return the number of messages examined
     */
    public int scan() throws Exception {
        MailSystemProperties.Imap cfg = properties.getImap();
        if (cfg.getUsername() == null || cfg.getUsername().isBlank()
                || cfg.getPassword() == null || cfg.getPassword().isBlank()) {
            log.debug("IMAP credentials not configured; skipping mailbox scan");
            return 0;
        }
        if (!scanning.compareAndSet(false, true)) {
            log.debug("A mailbox scan is already running; skipping");
            return 0;
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", cfg.getHost());
        props.put("mail.imaps.port", String.valueOf(cfg.getPort()));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "15000");
        props.put("mail.imaps.timeout", "30000");

        Session session = Session.getInstance(props);
        int examined = 0;
        try (Store store = session.getStore("imaps")) {
            store.connect(cfg.getHost(), cfg.getUsername(), cfg.getPassword());
            Folder folder = store.getFolder(cfg.getFolder());
            folder.open(Folder.READ_ONLY);
            try {
                // Only look at recent mail to keep scans cheap on large mailboxes.
                Date since = Date.from(Instant.now().minusSeconds(14L * 24 * 3600));
                Message[] messages = folder.search(new ReceivedDateTerm(ComparisonTerm.GE, since));
                for (Message message : messages) {
                    examined++;
                    try {
                        processMessage(message);
                    } catch (Exception ex) {
                        log.debug("Failed to process a message during scan: {}", ex.getMessage());
                    }
                }
            } finally {
                folder.close(false);
            }
        } finally {
            scanning.set(false);
        }
        log.info("Mailbox scan complete; examined {} message(s)", examined);
        return examined;
    }

    private void processMessage(Message message) throws Exception {
        if (isBounce(message)) {
            handleBounce(message);
        } else {
            handlePossibleReply(message);
        }
    }

    // ---- Bounce handling ---------------------------------------------------

    private boolean isBounce(Message message) throws Exception {
        String from = addressesToString(message.getFrom()).toLowerCase();
        String subject = Optional.ofNullable(message.getSubject()).orElse("").toLowerCase();
        String contentType = Optional.ofNullable(message.getContentType()).orElse("").toLowerCase();
        return from.contains("mailer-daemon") || from.contains("postmaster")
                || contentType.contains("report-type=delivery-status")
                || subject.contains("delivery status notification")
                || subject.contains("undeliverable")
                || subject.contains("returned mail")
                || subject.contains("mail delivery failed")
                || subject.contains("failure notice");
    }

    private void handleBounce(Message message) throws Exception {
        String text = MailContentExtractor.flattenToText(message);

        Optional<EmailDelivery> match = Optional.empty();

        // 1. Strongest signal: the X-Mailtrack-Id echoed inside the original message of the DSN.
        Matcher idMatcher = MAILTRACK_ID.matcher(text);
        if (idMatcher.find()) {
            match = deliveryRepository.findByTrackingId(idMatcher.group(1));
        }
        // 2. Fall back to the message-id embedded in the original message reference.
        if (match.isEmpty()) {
            Matcher msgIdMatcher = MAILTRACK_MSGID.matcher(text);
            if (msgIdMatcher.find()) {
                match = deliveryRepository.findByTrackingId(msgIdMatcher.group(1));
            }
        }
        // 3. Last resort: the failed recipient address.
        String failedRecipient = null;
        Matcher recipientMatcher = FINAL_RECIPIENT.matcher(text);
        if (recipientMatcher.find()) {
            failedRecipient = recipientMatcher.group(1).trim().toLowerCase();
        }
        if (match.isEmpty() && failedRecipient != null) {
            match = deliveryRepository.findFirstByRecipientEmailIgnoreCaseOrderBySentAtDesc(failedRecipient);
        }

        if (match.isEmpty()) {
            log.debug("Bounce message could not be matched to a delivery (failedRecipient={})",
                    failedRecipient);
            return;
        }

        EmailDelivery delivery = match.get();
        if (delivery.getStatus() == DeliveryStatus.BOUNCED) {
            return; // already recorded
        }

        String reason = "Delivery failed";
        Matcher diag = DIAGNOSTIC.matcher(text);
        if (diag.find()) {
            reason = diag.group(1).trim();
        }

        delivery.setStatus(DeliveryStatus.BOUNCED);
        delivery.setBouncedAt(receivedDate(message));
        delivery.setBounceReason(truncate(reason, 1990));
        deliveryRepository.save(delivery);
        log.info("Recorded BOUNCE for {} (reason: {})", delivery.getRecipientEmail(), reason);
    }

    // ---- Reply handling ----------------------------------------------------

    private void handlePossibleReply(Message message) throws Exception {
        String messageId = firstHeader(message, "Message-ID");
        if (messageId != null && replyRepository.existsByMessageId(messageId)) {
            return; // already imported
        }

        EmailDelivery delivery = matchReplyToDelivery(message);
        if (delivery == null) {
            return;
        }

        EmailReply reply = new EmailReply();
        reply.setDelivery(delivery);
        reply.setFromAddress(addressesToString(message.getFrom()));
        reply.setSubject(truncate(message.getSubject(), 990));
        reply.setSnippet(truncate(snippet(message), 4000));
        reply.setMessageId(messageId);
        reply.setReceivedAt(receivedDate(message));

        delivery.addReply(reply);
        if (delivery.getRepliedAt() == null) {
            delivery.setRepliedAt(reply.getReceivedAt());
        }
        if (delivery.getStatus() != DeliveryStatus.BOUNCED) {
            delivery.setStatus(DeliveryStatus.REPLIED);
            if (delivery.getDeliveredAt() == null) {
                delivery.setDeliveredAt(reply.getReceivedAt());
            }
        }
        deliveryRepository.save(delivery);
        log.info("Recorded REPLY from {} to delivery for {}",
                reply.getFromAddress(), delivery.getRecipientEmail());
    }

    private EmailDelivery matchReplyToDelivery(Message message) throws Exception {
        // 1. In-Reply-To / References pointing at one of our message-ids.
        String references = (firstHeader(message, "In-Reply-To") + " "
                + firstHeader(message, "References"));
        Matcher m = MAILTRACK_MSGID.matcher(references);
        if (m.find()) {
            Optional<EmailDelivery> byId = deliveryRepository.findByTrackingId(m.group(1));
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        // 2. Sender address + normalized subject matching a delivery we sent.
        String sender = primaryAddress(message.getFrom());
        if (sender == null) {
            return null;
        }
        Optional<EmailDelivery> bySender =
                deliveryRepository.findFirstByRecipientEmailIgnoreCaseOrderBySentAtDesc(sender.toLowerCase());
        if (bySender.isEmpty()) {
            return null;
        }
        EmailDelivery candidate = bySender.get();
        String replySubject = normalizeSubject(message.getSubject());
        String originalSubject = normalizeSubject(candidate.getCampaign().getSubject());
        // Require subject relatedness so unrelated mail from the same person is not attached.
        if (!replySubject.isEmpty() && replySubject.equals(originalSubject)) {
            return candidate;
        }
        return null;
    }

    // ---- Small helpers -----------------------------------------------------

    private static String normalizeSubject(String subject) {
        if (subject == null) {
            return "";
        }
        return subject.replaceAll("(?i)^(re|fwd|fw)\\s*:\\s*", "")
                .replaceAll("(?i)^(re|fwd|fw)\\s*:\\s*", "")
                .trim()
                .toLowerCase();
    }

    private static String firstHeader(Message message, String name) {
        try {
            String[] values = message.getHeader(name);
            return (values != null && values.length > 0) ? values[0] : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String addressesToString(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Address a : addresses) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(a.toString());
        }
        return sb.toString();
    }

    private static String primaryAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        if (addresses[0] instanceof InternetAddress ia) {
            return ia.getAddress();
        }
        return addresses[0].toString();
    }

    private static Instant receivedDate(Message message) {
        try {
            Date d = message.getReceivedDate();
            if (d == null) {
                d = message.getSentDate();
            }
            return d != null ? d.toInstant() : Instant.now();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static String snippet(Message message) {
        try {
            String text = MailContentExtractor.flattenToText(message);
            // Strip HTML tags and collapse whitespace for a readable preview.
            String plain = text.replaceAll("(?s)<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            return plain.length() > 1000 ? plain.substring(0, 1000) + "…" : plain;
        } catch (Exception e) {
            return "";
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
