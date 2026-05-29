package com.example.mailsystem;

import com.example.mailsystem.domain.Campaign;
import com.example.mailsystem.domain.DeliveryStatus;
import com.example.mailsystem.domain.EmailDelivery;
import com.example.mailsystem.repository.CampaignRepository;
import com.example.mailsystem.repository.EmailDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check that loading a delivery's tracking pixel records the open (read) and promotes
 * the delivery status.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TrackingIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CampaignRepository campaignRepository;

    @Autowired
    EmailDeliveryRepository deliveryRepository;

    @Test
    void loadingTrackingPixelRecordsOpen() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setSubject("Test");
        campaign.setBody("Body");
        EmailDelivery delivery = new EmailDelivery();
        delivery.setRecipientEmail("reader@example.com");
        delivery.setTrackingId("track-it-123");
        delivery.setStatus(DeliveryStatus.SENT);
        campaign.addDelivery(delivery);
        campaignRepository.save(campaign);

        mockMvc.perform(get("/track/open/track-it-123.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        EmailDelivery reloaded = deliveryRepository.findByTrackingId("track-it-123").orElseThrow();
        assertNotNull(reloaded.getFirstOpenedAt(), "open should be timestamped");
        assertNotNull(reloaded.getDeliveredAt(), "an open should also prove receipt");
        assertEquals(1, reloaded.getOpenCount());
        assertEquals(DeliveryStatus.READ, reloaded.getStatus());
    }
}
