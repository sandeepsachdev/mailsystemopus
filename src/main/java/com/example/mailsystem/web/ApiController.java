package com.example.mailsystem.web;

import com.example.mailsystem.domain.Campaign;
import com.example.mailsystem.domain.MailingList;
import com.example.mailsystem.dto.CampaignView;
import com.example.mailsystem.repository.CampaignRepository;
import com.example.mailsystem.service.MailSendingService;
import com.example.mailsystem.service.MailboxScanService;
import com.example.mailsystem.service.MailingListService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON API mirroring the UI: manage lists, send campaigns, read per-recipient tracking, and
 * trigger a mailbox scan for bounces/replies.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final CampaignRepository campaignRepository;
    private final MailingListService mailingListService;
    private final MailSendingService mailSendingService;
    private final MailboxScanService mailboxScanService;

    public ApiController(CampaignRepository campaignRepository,
                         MailingListService mailingListService,
                         MailSendingService mailSendingService,
                         MailboxScanService mailboxScanService) {
        this.campaignRepository = campaignRepository;
        this.mailingListService = mailingListService;
        this.mailSendingService = mailSendingService;
        this.mailboxScanService = mailboxScanService;
    }

    // ---- Mailing lists -----------------------------------------------------

    public record MailingListRequest(@NotBlank String name, String description, String recipients) {
    }

    @GetMapping("/lists")
    public List<MailingList> lists() {
        return mailingListService.findAll();
    }

    @PostMapping("/lists")
    public MailingList createList(@RequestBody MailingListRequest req) {
        return mailingListService.create(req.name(), req.description(), req.recipients());
    }

    // ---- Campaigns ---------------------------------------------------------

    public record SendRequest(@NotBlank String subject, @NotBlank String body,
                              Long mailingListId, String recipients) {
    }

    @GetMapping("/campaigns")
    public List<CampaignView> campaigns() {
        return campaignRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CampaignView::summary).toList();
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<CampaignView> campaign(@PathVariable Long id) {
        return campaignRepository.findById(id)
                .map(c -> ResponseEntity.ok(CampaignView.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/campaigns")
    public CampaignView send(@RequestBody SendRequest req) {
        Set<String> recipients = new java.util.LinkedHashSet<>();
        String listName = null;
        if (req.mailingListId() != null) {
            MailingList list = mailingListService.get(req.mailingListId());
            recipients.addAll(list.getRecipients());
            listName = list.getName();
        }
        recipients.addAll(mailingListService.parseAddresses(req.recipients()));
        Campaign campaign = mailSendingService.sendCampaign(req.subject(), req.body(), listName, recipients);
        return CampaignView.from(campaign);
    }

    // ---- Mailbox scan ------------------------------------------------------

    @PostMapping("/scan")
    public Map<String, Object> scan() throws Exception {
        int examined = mailboxScanService.scan();
        return Map.of("examined", examined);
    }

    /**
     * Returns configuration/validation problems (e.g. missing Gmail credentials) as a clean
     * 400 with the actionable message instead of an opaque 500.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
