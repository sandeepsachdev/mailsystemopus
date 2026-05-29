package com.example.mailsystem.service;

import com.example.mailsystem.domain.MailingList;
import com.example.mailsystem.repository.MailingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD operations for mailing lists and their recipient addresses.
 */
@Service
@Transactional
public class MailingListService {

    private final MailingListRepository repository;

    public MailingListService(MailingListRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MailingList> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public MailingList get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mailing list not found: " + id));
    }

    public MailingList create(String name, String description, String recipientsRaw) {
        if (repository.existsByName(name)) {
            throw new IllegalArgumentException("A mailing list named '" + name + "' already exists");
        }
        MailingList list = new MailingList();
        list.setName(name);
        list.setDescription(description);
        list.setRecipients(parseAddresses(recipientsRaw));
        return repository.save(list);
    }

    public MailingList update(Long id, String name, String description, String recipientsRaw) {
        MailingList list = get(id);
        list.setName(name);
        list.setDescription(description);
        list.setRecipients(parseAddresses(recipientsRaw));
        return repository.save(list);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** Splits a raw textarea/CSV value into a de-duplicated, validated set of addresses. */
    public Set<String> parseAddresses(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(this::looksLikeEmail)
                .map(s -> s.toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean looksLikeEmail(String s) {
        int at = s.indexOf('@');
        return at > 0 && at < s.length() - 1 && s.indexOf('.', at) > at;
    }
}
