package com.example.mailsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding of the {@code mailsystem.*} configuration in application.yml.
 */
@ConfigurationProperties(prefix = "mailsystem")
public class MailSystemProperties {

    private String baseUrl = "http://localhost:8080";
    private String fromAddress = "";
    private String fromName = "Mail System";
    private int deliveredGraceMinutes = 10;
    private final Imap imap = new Imap();

    public static class Imap {
        private boolean enabled = true;
        private String host = "imap.gmail.com";
        private int port = 993;
        private String username = "";
        private String password = "";
        private String folder = "INBOX";
        private long pollIntervalMs = 60000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFolder() {
            return folder;
        }

        public void setFolder(String folder) {
            this.folder = folder;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public int getDeliveredGraceMinutes() {
        return deliveredGraceMinutes;
    }

    public void setDeliveredGraceMinutes(int deliveredGraceMinutes) {
        this.deliveredGraceMinutes = deliveredGraceMinutes;
    }

    public Imap getImap() {
        return imap;
    }
}
