package com.example.mailsystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persists the high-water mark of the last IMAP mailbox scan so subsequent scans only examine
 * messages that have arrived since, instead of re-reading a fixed trailing window every time.
 *
 * <p>IMAP UIDs are only stable within a given {@code UIDVALIDITY} for a folder; if the server
 * reports a different validity (or the folder changes), the stored {@code lastUid} is discarded
 * and the next scan re-bootstraps from a date window.
 */
@Entity
@Table(name = "scan_state")
public class ScanState {

    /** Single-row table: always id = 1. */
    @Id
    private Long id = 1L;

    private String folder;

    /** The folder's UIDVALIDITY the {@link #lastUid} was recorded under. */
    private long uidValidity;

    /** Highest IMAP UID processed so far. */
    private long lastUid;

    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public long getUidValidity() {
        return uidValidity;
    }

    public void setUidValidity(long uidValidity) {
        this.uidValidity = uidValidity;
    }

    public long getLastUid() {
        return lastUid;
    }

    public void setLastUid(long lastUid) {
        this.lastUid = lastUid;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
