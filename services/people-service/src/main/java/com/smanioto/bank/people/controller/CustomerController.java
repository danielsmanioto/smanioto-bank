package com.smanioto.bank.people.controller;

import com.smanioto.bank.people.dto.CreateCustomerRequest;
import com.smanioto.bank.people.dto.CustomerExistsResponse;
import com.smanioto.bank.people.dto.CustomerResponse;
import com.smanioto.bank.people.service.CustomerService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/people")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        var customer = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CustomerResponse(customer.getId(), customer.getFullName(), customer.getCpf(), customer.getEmail()));
    }

    @GetMapping("/{customerId}/exists")
    public CustomerExistsResponse exists(@PathVariable UUID customerId) {
        return new CustomerExistsResponse(customerService.exists(customerId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
