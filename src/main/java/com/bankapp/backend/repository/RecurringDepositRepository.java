package com.bankapp.backend.repository;

import com.bankapp.backend.entity.RecurringDeposit;
import com.bankapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, Long> {
    List<RecurringDeposit> findByOwner(User owner);
    List<RecurringDeposit> findByMaturityDateAndStatus(LocalDate maturityDate, com.bankapp.backend.entity.DepositStatus status);
}
