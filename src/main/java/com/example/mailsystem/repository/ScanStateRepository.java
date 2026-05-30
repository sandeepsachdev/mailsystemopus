package com.example.mailsystem.repository;

import com.example.mailsystem.domain.ScanState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanStateRepository extends JpaRepository<ScanState, Long> {
}
