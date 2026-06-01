package com.smanioto.bank.accounts.integration;

import java.util.UUID;

public interface PeopleClient {

    boolean customerExists(UUID customerId);
}
