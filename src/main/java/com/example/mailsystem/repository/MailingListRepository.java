package com.example.mailsystem.repository;

import com.example.mailsystem.domain.MailingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailingListRepository extends JpaRepository<MailingList, Long> {
    Optional<MailingList> findByName(String name);
    boolean existsByName(String name);
}
