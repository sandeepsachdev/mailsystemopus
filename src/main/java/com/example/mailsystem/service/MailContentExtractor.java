package com.example.mailsystem.service;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helpers for flattening a (possibly multipart) MIME message into plain text so it can be scanned
 * for bounce diagnostics and recipient/header information.
 */
final class MailContentExtractor {

    private MailContentExtractor() {
    }

    /** Recursively concatenates the textual content of every part of a message. */
    static String flattenToText(Part part) {
        StringBuilder sb = new StringBuilder();
        collect(part, sb, 0);
        return sb.toString();
    }

    private static void collect(Part part, StringBuilder sb, int depth) {
        if (part == null || depth > 12) {
            return;
        }
        try {
            Object content = part.getContent();
            if (content instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    collect(multipart.getBodyPart(i), sb, depth + 1);
                }
            } else if (content instanceof Message nested) {
                // message/rfc822 part: include its raw headers (carries X-Mailtrack-Id etc.).
                appendHeaders(nested, sb);
                collect(nested, sb, depth + 1);
            } else if (content instanceof String text) {
                sb.append(text).append('\n');
            } else if (content instanceof InputStream in) {
                sb.append(new String(in.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
            }
        } catch (Exception ignored) {
            // Best-effort: a part we cannot decode simply contributes nothing.
        }
    }

    private static void appendHeaders(Message message, StringBuilder sb) {
        try {
            var headers = message.getAllHeaders();
            while (headers.hasMoreElements()) {
                var h = headers.nextElement();
                sb.append(h.getName()).append(": ").append(h.getValue()).append('\n');
            }
        } catch (Exception ignored) {
            // ignore
        }
    }
}
