package com.example.mailsystem.web;

import com.example.mailsystem.service.TrackingService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

/**
 * Serves the 1x1 tracking pixel embedded in outgoing emails. When a recipient's mail client loads
 * the image, the corresponding delivery is marked as opened (read).
 */
@RestController
public class TrackingController {

    /** A 1x1 transparent PNG. */
    private static final byte[] PIXEL = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping(value = {"/track/open/{trackingId}.png", "/track/open/{trackingId}"},
            produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> open(@PathVariable String trackingId) {
        try {
            trackingService.recordOpen(trackingId);
        } catch (Exception ignored) {
            // Never let tracking failures prevent the pixel from loading.
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(PIXEL);
    }
}
