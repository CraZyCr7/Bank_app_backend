package com.bankapp.backend.repository;

import com.bankapp.backend.entity.FailedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedTransactionRepository extends JpaRepository<FailedTransaction, Long> {
}
