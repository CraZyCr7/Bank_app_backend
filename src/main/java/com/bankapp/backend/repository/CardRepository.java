package com.bankapp.backend.repository;

import com.bankapp.backend.entity.Card;
import com.bankapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByOwner(User owner);
    Optional<Card> findByIdAndOwner(Long id, User owner);
    Optional<Card> findByCardNumber(String cardNumber);
}
