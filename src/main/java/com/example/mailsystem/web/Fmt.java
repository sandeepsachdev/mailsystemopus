package com.example.mailsystem.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Null-safe date/time formatting helpers callable from Thymeleaf via the {@code T(...)} syntax,
 * e.g. {@code ${T(com.example.mailsystem.web.Fmt).dt(delivery.sentAt)}}. Avoids the limitations
 * of formatting {@link Instant} (which has no calendar fields) through the temporals dialect.
 */
public final class Fmt {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private Fmt() {
    }

    /** Formats an instant as {@code yyyy-MM-dd HH:mm} in the server's zone, or "—" when null. */
    public static String dt(Instant instant) {
        return instant == null ? "—" : DT.format(instant);
    }
}
