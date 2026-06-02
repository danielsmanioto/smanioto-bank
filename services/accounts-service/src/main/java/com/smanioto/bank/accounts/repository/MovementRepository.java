package com.smanioto.bank.accounts.repository;

import com.smanioto.bank.accounts.model.Movement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    List<Movement> findByAccountIdOrderByCreatedAtAscIdAsc(UUID accountId);
}
