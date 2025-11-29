package com.bankapp.backend.repository;

import com.bankapp.backend.entity.Beneficiary;
import com.bankapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByOwner(User owner);

    Optional<Beneficiary> findByIdAndOwner(Long id, User owner);

    boolean existsByOwnerAndAccountNumber(User owner, String accountNumber);
}
