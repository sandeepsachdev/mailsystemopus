package com.example.mailsystem.config;

import com.example.mailsystem.domain.MailingList;
import com.example.mailsystem.repository.MailingListRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Seeds a sample mailing list on first run so the UI is not empty during evaluation.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(MailingListRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            MailingList list = new MailingList();
            list.setName("Sample List");
            list.setDescription("Example mailing list — edit or delete me.");
            Set<String> recipients = new LinkedHashSet<>();
            recipients.add("example.recipient@gmail.com");
            list.setRecipients(recipients);
            repository.save(list);
        };
    }
}
