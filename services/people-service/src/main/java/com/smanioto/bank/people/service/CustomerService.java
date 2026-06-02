package com.smanioto.bank.people.service;

import com.smanioto.bank.people.dto.CreateCustomerRequest;
import com.smanioto.bank.people.model.Customer;
import com.smanioto.bank.people.repository.CustomerRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer create(CreateCustomerRequest request) {
        try {
            return customerRepository.save(new Customer(
                UUID.randomUUID(),
                request.fullName().trim(),
                request.cpf(),
                request.email().trim().toLowerCase()
            ));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Cliente já cadastrado com CPF ou e-mail informado");
        }
    }

    public boolean exists(UUID customerId) {
        return customerRepository.existsById(customerId);
    }
}
