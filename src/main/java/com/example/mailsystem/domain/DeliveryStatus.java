package com.example.mailsystem.domain;

/**
 * Lifecycle state of a single email sent to a single recipient.
 *
 * <p>The states are not a strict linear progression: an email may go SENT -&gt; BOUNCED, or
 * SENT -&gt; RECEIVED -&gt; READ -&gt; REPLIED. The most "advanced" known state is kept on the
 * delivery while individual timestamps are recorded independently.
 */
public enum DeliveryStatus {
    /** Handed off to Gmail's SMTP server successfully (queued for delivery). */
    SENT,
    /** Delivered to the recipient's mailbox (inferred from no bounce, or proven by an open). */
    RECEIVED,
    /** The recipient opened the message (recorded via the tracking pixel). */
    READ,
    /** The recipient replied to the message. */
    REPLIED,
    /** Delivery permanently failed; a bounce (DSN) was read back from the mailbox. */
    BOUNCED,
    /** Sending failed locally before/while handing off to SMTP. */
    FAILED
}
