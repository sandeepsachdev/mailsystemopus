package com.example.mailsystem;

import com.example.mailsystem.service.MailingListService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for address parsing — the repository is not exercised here.
 */
class MailingListServiceTest {

    private final MailingListService service = new MailingListService(null);

    @Test
    void parsesMixedSeparatorsAndLowercases() {
        Set<String> result = service.parseAddresses("Alice@Example.com, bob@x.com;  carol@y.com\n dave@z.com");
        assertEquals(Set.of("alice@example.com", "bob@x.com", "carol@y.com", "dave@z.com"), result);
    }

    @Test
    void dropsInvalidAddressesAndDuplicates() {
        Set<String> result = service.parseAddresses("good@mail.com, notanemail, good@mail.com, also-bad@");
        assertEquals(Set.of("good@mail.com"), result);
    }

    @Test
    void handlesNullAndBlank() {
        assertTrue(service.parseAddresses(null).isEmpty());
        assertTrue(service.parseAddresses("   ").isEmpty());
    }
}
