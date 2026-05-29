package com.example.mailsystem.web;

import com.example.mailsystem.domain.Campaign;
import com.example.mailsystem.domain.MailingList;
import com.example.mailsystem.repository.CampaignRepository;
import com.example.mailsystem.service.MailSendingService;
import com.example.mailsystem.service.MailboxScanService;
import com.example.mailsystem.service.MailingListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Thymeleaf-backed web UI: compose and send campaigns, manage mailing lists, view per-recipient
 * delivery tracking, and trigger a manual mailbox scan.
 */
@Controller
public class DashboardController {

    private final CampaignRepository campaignRepository;
    private final MailingListService mailingListService;
    private final MailSendingService mailSendingService;
    private final MailboxScanService mailboxScanService;

    public DashboardController(CampaignRepository campaignRepository,
                              MailingListService mailingListService,
                              MailSendingService mailSendingService,
                              MailboxScanService mailboxScanService) {
        this.campaignRepository = campaignRepository;
        this.mailingListService = mailingListService;
        this.mailSendingService = mailSendingService;
        this.mailboxScanService = mailboxScanService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/campaigns";
    }

    // ---- Campaigns ---------------------------------------------------------

    @GetMapping("/campaigns")
    public String campaigns(Model model) {
        model.addAttribute("campaigns", campaignRepository.findAllByOrderByCreatedAtDesc());
        return "campaigns";
    }

    @GetMapping("/campaigns/{id}")
    public String campaignDetail(@PathVariable Long id, Model model) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));
        model.addAttribute("campaign", campaign);
        return "campaign-detail";
    }

    @GetMapping("/compose")
    public String compose(Model model) {
        model.addAttribute("lists", mailingListService.findAll());
        return "compose";
    }

    @PostMapping("/campaigns")
    public String send(@RequestParam String subject,
                       @RequestParam String body,
                       @RequestParam(required = false) Long mailingListId,
                       @RequestParam(required = false) String extraRecipients,
                       RedirectAttributes ra) {
        Set<String> recipients = new LinkedHashSet<>();
        String listName = null;
        if (mailingListId != null) {
            MailingList list = mailingListService.get(mailingListId);
            recipients.addAll(list.getRecipients());
            listName = list.getName();
        }
        recipients.addAll(mailingListService.parseAddresses(extraRecipients));

        if (recipients.isEmpty()) {
            ra.addFlashAttribute("error", "No valid recipients were provided.");
            return "redirect:/compose";
        }

        try {
            Campaign campaign = mailSendingService.sendCampaign(subject, body, listName, recipients);
            ra.addFlashAttribute("message",
                    "Campaign sent to " + campaign.getTotalCount() + " recipient(s).");
            return "redirect:/campaigns/" + campaign.getId();
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to send: " + ex.getMessage());
            return "redirect:/compose";
        }
    }

    @PostMapping("/scan")
    public String scan(RedirectAttributes ra) {
        try {
            int examined = mailboxScanService.scan();
            ra.addFlashAttribute("message",
                    "Mailbox scan complete. Examined " + examined + " message(s) for bounces and replies.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Mailbox scan failed: " + ex.getMessage());
        }
        return "redirect:/campaigns";
    }

    // ---- Mailing lists -----------------------------------------------------

    @GetMapping("/lists")
    public String lists(Model model) {
        model.addAttribute("lists", mailingListService.findAll());
        return "lists";
    }

    @GetMapping("/lists/new")
    public String newList(Model model) {
        model.addAttribute("list", new MailingList());
        return "list-form";
    }

    @GetMapping("/lists/{id}/edit")
    public String editList(@PathVariable Long id, Model model) {
        model.addAttribute("list", mailingListService.get(id));
        return "list-form";
    }

    @PostMapping("/lists")
    public String createList(@RequestParam String name,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String recipients,
                             RedirectAttributes ra) {
        try {
            mailingListService.create(name, description, recipients);
            ra.addFlashAttribute("message", "Mailing list '" + name + "' created.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/lists";
    }

    @PostMapping("/lists/{id}")
    public String updateList(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String recipients,
                             RedirectAttributes ra) {
        try {
            mailingListService.update(id, name, description, recipients);
            ra.addFlashAttribute("message", "Mailing list '" + name + "' updated.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/lists";
    }

    @PostMapping("/lists/{id}/delete")
    public String deleteList(@PathVariable Long id, RedirectAttributes ra) {
        mailingListService.delete(id);
        ra.addFlashAttribute("message", "Mailing list deleted.");
        return "redirect:/lists";
    }
}
