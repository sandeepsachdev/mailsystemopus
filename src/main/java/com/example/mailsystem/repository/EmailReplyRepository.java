package com.example.mailsystem.repository;

import com.example.mailsystem.domain.EmailReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailReplyRepository extends JpaRepository<EmailReply, Long> {
    boolean existsByMessageId(String messageId);
}
