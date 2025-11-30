package com.bankapp.backend.repository;

import com.bankapp.backend.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    Optional<TransactionRecord> findByReference(String reference);
}
