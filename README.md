# Gmail Mailing System

A Spring Boot application that **sends** emails through Gmail and **reads the Gmail mailbox back**
to track, for every campaign, how many recipients **received**, **read**, **bounced**, and
**replied** — including *which* addresses and *when* each event happened.

## Features

- **Mailing lists** — create/edit/delete reusable lists of recipient addresses (de-duplicated and validated).
- **Send campaigns** to a mailing list, ad-hoc addresses, or both. One MIME message is sent per
  recipient so each can be tracked independently.
- **Per-recipient tracking** with timestamps:
  - **Sent** — handed off to Gmail SMTP.
  - **Received** — proven by an open, or inferred once a configurable grace period passes with no bounce.
  - **Read** — recorded via a 1×1 tracking pixel embedded in the email (`/track/open/{id}.png`).
  - **Bounced** — detected by reading **bounce / Delivery-Status-Notification** messages back from
    Gmail over IMAP; the failed recipient and reason are parsed and matched to the delivery.
  - **Replied** — replies are read back over IMAP and shown under the recipient they came from.
- **Dashboard** (Thymeleaf) showing aggregate counts per campaign and a per-recipient breakdown,
  plus a **JSON REST API** under `/api`.
- **Automatic background mailbox polling** plus an on-demand **“Sync mailbox”** button.

## How tracking works

| Signal | Mechanism |
|--------|-----------|
| Read | A unique tracking pixel per delivery. When the recipient's client loads the image, the open is recorded (`firstOpenedAt`, `openCount`). |
| Received | A loaded pixel proves receipt; otherwise a delivery with no bounce after `mailsystem.delivered-grace-minutes` is marked received. |
| Bounced | IMAP scan looks for mailer-daemon / DSN messages, parses `Final-Recipient`, the echoed `X-Mailtrack-Id` header, and the diagnostic, and flags the matching delivery `BOUNCED`. |
| Replied | IMAP scan matches `In-Reply-To`/`References` against our message-ids, or sender + normalized subject, then stores the reply text. |

## Prerequisites

- Java 21+, Maven 3.9+
- A Gmail account with **2-Step Verification** enabled and a **16-character App Password**
  (create one at <https://myaccount.google.com/apppasswords>). Gmail no longer allows plain
  passwords for SMTP/IMAP.
- IMAP must be enabled in Gmail (Settings → Forwarding and POP/IMAP → Enable IMAP).

## Configuration

Set these environment variables (or edit `src/main/resources/application.yml`):

```bash
export GMAIL_USERNAME="your.account@gmail.com"
export GMAIL_APP_PASSWORD="abcd efgh ijkl mnop"   # the App Password, spaces optional
# For opens to be recorded by remote mail clients, BASE_URL must be publicly reachable:
export MAILSYSTEM_BASE_URL="http://localhost:8080"
```

## Run

```bash
mvn spring-boot:run
# then open http://localhost:8080
```

- `GET /` → campaigns dashboard
- `/compose` → send a campaign
- `/lists` → manage mailing lists
- “Sync mailbox” (top right) → read Gmail now for bounces & replies

### REST API

```bash
# Create a list
curl -X POST localhost:8080/api/lists -H 'Content-Type: application/json' \
  -d '{"name":"Team","recipients":"a@x.com, b@y.com"}'

# Send a campaign
curl -X POST localhost:8080/api/campaigns -H 'Content-Type: application/json' \
  -d '{"subject":"Hello","body":"Hi there","mailingListId":1,"recipients":"extra@z.com"}'

# Read per-recipient tracking
curl localhost:8080/api/campaigns/1

# Trigger a mailbox scan
curl -X POST localhost:8080/api/scan
```

## Notes & limitations

- **Open tracking** depends on the recipient's mail client loading remote images; many clients
  block them by default, so a missing “read” does not necessarily mean the mail was unread.
- Gmail rewrites the outgoing `Message-ID`, so reply matching falls back to sender + subject when
  the original id is not referenced.
- H2 (file-based, `./data/`) is used for storage out of the box; point `spring.datasource.*` at
  another database for production.
- Build/run requires network access to Maven Central for first-time dependency download.

## Project layout

```
domain/      JPA entities: MailingList, Campaign, EmailDelivery, EmailReply, DeliveryStatus
repository/  Spring Data repositories
service/     MailSendingService (SMTP), MailboxScanService (IMAP bounces/replies),
             TrackingService (opens + delivered-after-grace), MailingListService
web/         DashboardController (Thymeleaf UI), ApiController (JSON), TrackingController (pixel)
config/      Properties binding + sample data seeding
templates/   Thymeleaf views
```
