package com.smanioto.bank.accounts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smanioto.bank.accounts.integration.PeopleClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PeopleClient peopleClient;

    @Test
    void shouldOpenAccountWhenCustomerExists() throws Exception {
        given(peopleClient.customerExists(any())).willReturn(true);
        String customerId = UUID.randomUUID().toString();

        var body = """
            {
              "customerId": "%s",
              "bank": "341",
              "branch": "0001",
              "number": "12345-6",
              "initialBalance": 250.00
            }
            """.formatted(customerId);

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value(customerId))
            .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void shouldRejectAccountOpeningWhenCustomerDoesNotExist() throws Exception {
        given(peopleClient.customerExists(any())).willReturn(false);

        var body = """
            {
              "customerId": "%s",
              "bank": "341",
              "branch": "0001",
              "number": "12345-6",
              "initialBalance": 0.00
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldTransferAndListStatementInChronologicalOrder() throws Exception {
        given(peopleClient.customerExists(any())).willReturn(true);
        String sourceAccountId = openAccount("11111-1", 1000.00);
        String destinationAccountId = openAccount("22222-2", 300.00);

        var transferBody = """
            {
              "fromAccountId": "%s",
              "toAccountId": "%s",
              "amount": 120.00
            }
            """.formatted(sourceAccountId, destinationAccountId);

        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(120.00));

        mockMvc.perform(get("/accounts/{accountId}/statement", sourceAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("DEBIT"))
            .andExpect(jsonPath("$[0].amount").value(120.00));

        mockMvc.perform(get("/accounts/{accountId}/statement", destinationAccountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("CREDIT"))
            .andExpect(jsonPath("$[0].amount").value(120.00));
    }

    @Test
    void shouldRejectTransferWhenValidationFails() throws Exception {
        given(peopleClient.customerExists(any())).willReturn(true);
        String sourceAccountId = openAccount("33333-3", 40.00);
        String destinationAccountId = openAccount("44444-4", 10.00);

        var insufficientBalanceBody = """
            {
              "fromAccountId": "%s",
              "toAccountId": "%s",
              "amount": 100.00
            }
            """.formatted(sourceAccountId, destinationAccountId);
        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(insufficientBalanceBody))
            .andExpect(status().isBadRequest());

        var invalidAmountBody = """
            {
              "fromAccountId": "%s",
              "toAccountId": "%s",
              "amount": 0.00
            }
            """.formatted(sourceAccountId, destinationAccountId);
        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidAmountBody))
            .andExpect(status().isBadRequest());

        var missingDestinationBody = """
            {
              "fromAccountId": "%s",
              "toAccountId": "%s",
              "amount": 5.00
            }
            """.formatted(sourceAccountId, UUID.randomUUID());
        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(missingDestinationBody))
            .andExpect(status().isBadRequest());

        var sameAccountBody = """
            {
              "fromAccountId": "%s",
              "toAccountId": "%s",
              "amount": 1.00
            }
            """.formatted(sourceAccountId, sourceAccountId);
        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sameAccountBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransferBetweenDifferentBanks() throws Exception {
        given(peopleClient.customerExists(any())).willReturn(true);
        String sourceAccountId = openAccount("55555-5", 100.00, "341");
        String destinationAccountId = openAccount("66666-6", 20.00, "237");

        var transferBody = """
            {
              "fromAccountId": "%s",
              "toAccountId": "%s",
              "amount": 10.00
            }
            """.formatted(sourceAccountId, destinationAccountId);

        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody))
            .andExpect(status().isBadRequest());
    }

    private String openAccount(String accountNumber, double balance) throws Exception {
        return openAccount(accountNumber, balance, "341");
    }

    private String openAccount(String accountNumber, double balance, String bank) throws Exception {
        var body = """
            {
              "customerId": "%s",
              "bank": "%s",
              "branch": "0001",
              "number": "%s",
              "initialBalance": %.2f
            }
            """.formatted(UUID.randomUUID(), bank, accountNumber, balance);

        var result = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        return responseJson.get("id").asText();
    }
}
