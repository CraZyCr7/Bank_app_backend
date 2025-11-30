package com.bankapp.backend.repository;

import com.bankapp.backend.entity.FixedDeposit;
import com.bankapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {
    List<FixedDeposit> findByOwner(User owner);
    List<FixedDeposit> findByMaturityDateAndStatus(LocalDate maturityDate, com.bankapp.backend.entity.DepositStatus status);
}
