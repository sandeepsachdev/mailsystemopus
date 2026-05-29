package com.example.mailsystem.service;

import com.example.mailsystem.config.MailSystemProperties;
import com.example.mailsystem.domain.Campaign;
import com.example.mailsystem.domain.DeliveryStatus;
import com.example.mailsystem.domain.EmailDelivery;
import com.example.mailsystem.repository.CampaignRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Builds and sends campaign emails through Gmail SMTP, one message per recipient so each can be
 * tracked independently. Every message embeds:
 * <ul>
 *   <li>a 1x1 tracking pixel pointing at {@code /track/open/{trackingId}} to detect opens (reads);</li>
 *   <li>an {@code X-Mailtrack-Id} header carrying the same token, which is echoed back inside
 *       bounce DSNs and so lets the bounce be matched to the exact delivery.</li>
 * </ul>
 */
@Service
public class MailSendingService {

    private static final Logger log = LoggerFactory.getLogger(MailSendingService.class);

    private final JavaMailSender mailSender;
    private final CampaignRepository campaignRepository;
    private final MailSystemProperties properties;

    public MailSendingService(JavaMailSender mailSender,
                              CampaignRepository campaignRepository,
                              MailSystemProperties properties) {
        this.mailSender = mailSender;
        this.campaignRepository = campaignRepository;
        this.properties = properties;
    }

    /**
     * Creates a campaign and sends it to every supplied recipient. The campaign (with one
     * {@link EmailDelivery} per recipient and their individual send results) is persisted and returned.
     */
    @Transactional
    public Campaign sendCampaign(String subject, String body, String mailingListName,
                                 Collection<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }

        Campaign campaign = new Campaign();
        campaign.setSubject(subject);
        campaign.setBody(body);
        campaign.setMailingListName(mailingListName);
        campaign.setSentAt(Instant.now());

        for (String recipient : recipients) {
            EmailDelivery delivery = new EmailDelivery();
            delivery.setRecipientEmail(recipient.trim().toLowerCase());
            delivery.setTrackingId(UUID.randomUUID().toString().replace("-", ""));
            campaign.addDelivery(delivery);
        }

        // Persist first so deliveries have IDs and the campaign exists before network I/O.
        campaign = campaignRepository.save(campaign);

        for (EmailDelivery delivery : campaign.getDeliveries()) {
            sendOne(campaign, delivery);
        }
        return campaignRepository.save(campaign);
    }

    private void sendOne(Campaign campaign, EmailDelivery delivery) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String from = properties.getFromAddress();
            if (from != null && !from.isBlank()) {
                helper.setFrom(new InternetAddress(from, properties.getFromName()));
            }
            helper.setTo(delivery.getRecipientEmail());
            helper.setSubject(campaign.getSubject());
            helper.setText(buildHtmlBody(campaign.getBody(), delivery.getTrackingId()), true);

            String messageId = "<" + delivery.getTrackingId() + "@mailsystem.local>";
            // Custom header survives in the original message echoed inside a bounce DSN.
            message.setHeader("X-Mailtrack-Id", delivery.getTrackingId());
            message.setHeader("Message-ID", messageId);
            // Best-effort read receipt request (recipient must opt in; pixel is the reliable path).
            if (from != null && !from.isBlank()) {
                message.setHeader("Disposition-Notification-To", from);
            }

            mailSender.send(message);

            delivery.setMessageId(messageId);
            delivery.setSentAt(Instant.now());
            delivery.setStatus(DeliveryStatus.SENT);
            log.info("Sent campaign {} to {} (trackingId={})",
                    campaign.getId(), delivery.getRecipientEmail(), delivery.getTrackingId());
        } catch (Exception ex) {
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setErrorMessage(truncate(ex.getMessage(), 1990));
            log.error("Failed to send campaign {} to {}: {}",
                    campaign.getId(), delivery.getRecipientEmail(), ex.getMessage());
        }
    }

    private String buildHtmlBody(String body, String trackingId) {
        String pixelUrl = properties.getBaseUrl() + "/track/open/" + trackingId + ".png";
        String safeBody = body == null ? "" : body;
        // If the body is plain text, preserve newlines; if it already contains markup, leave it.
        String rendered = safeBody.contains("<") && safeBody.contains(">")
                ? safeBody
                : "<p>" + HtmlUtils.htmlEscape(safeBody).replace("\n", "<br/>") + "</p>";
        return "<html><body>" + rendered
                + "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" alt=\"\" "
                + "style=\"display:none\"/></body></html>";
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
