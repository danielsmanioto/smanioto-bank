package com.smanioto.bank.people.repository;

import com.smanioto.bank.people.model.Customer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
