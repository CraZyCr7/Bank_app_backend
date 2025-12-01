package com.bankapp.backend.repository;

import com.bankapp.backend.entity.Account;
import com.bankapp.backend.entity.AccountType;
import com.bankapp.backend.entity.User;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwner(User owner);

    Optional<Account> findByIdAndOwner(Long id, User owner);
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByOwnerAndAccountType(User owner, AccountType accountType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

}
