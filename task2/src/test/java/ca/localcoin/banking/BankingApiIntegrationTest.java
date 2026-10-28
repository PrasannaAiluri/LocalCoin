package ca.localcoin.banking;

import ca.localcoin.banking.domain.AccountBalance;
import ca.localcoin.banking.domain.Branch;
import ca.localcoin.banking.repository.AccountBalanceRepository;
import ca.localcoin.banking.repository.BranchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankingApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private BranchRepository branchRepository;
    @Autowired private AccountBalanceRepository balanceRepository;
    @Autowired private ObjectMapper objectMapper;

    private Long branchId;

    @BeforeEach
    void createBranch() {
        // Use a unique branch code so each test can run independently.
        Branch branch = new Branch("TEST-" + UUID.randomUUID().toString().substring(0, 8),
                "Test Branch", "10 Main Street", "Toronto", "M5V1A1", "CA");
        branchId = branchRepository.save(branch).getId();
    }

    @Test
    void exposesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Local Coin Banking API"))
                .andExpect(jsonPath("$.paths['/api/v1/accounts']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/withdrawals']").exists());
    }

    @Test
    void opensAnAccountAndReturnsItsDetails() throws Exception {
        String response = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON).content(openAccountJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        long accountId = json.get("accountId").asLong();
        mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.availableBalance").value(0.00));
    }

    @Test
    void withdrawalUpdatesTheBalanceAndARepeatDoesNotDebitTwice() throws Exception {
        long accountId = openAccount();

        // Give the account test funds directly; deposits are outside the requested API scope.
        AccountBalance balance = balanceRepository.findById(accountId).get();
        balance.credit(new java.math.BigDecimal("100.00"));
        balanceRepository.saveAndFlush(balance);

        String withdrawalKey = "withdrawal-" + UUID.randomUUID();
        String body = moneyJson("25.00", withdrawalKey);
        mockMvc.perform(post("/api/v1/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replayed").value(false));

        mockMvc.perform(post("/api/v1/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replayed").value(true));

        mockMvc.perform(get("/api/v1/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(75.00))
                .andExpect(jsonPath("$.ledgerBalance").value(75.00));
    }

    @Test
    void withdrawalWithInsufficientFundsIsRejected() throws Exception {
        long accountId = openAccount();
        mockMvc.perform(post("/api/v1/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moneyJson("1.00", "empty-withdrawal-" + UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void missingAccountReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/999999999"))
                .andExpect(status().isNotFound());
    }

    private long openAccount() throws Exception {
        String response = mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON).content(openAccountJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accountId").asLong();
    }

    private String openAccountJson() {
        return "{"
                + "\"firstName\":\"Asha\","
                + "\"middleName\":null,"
                + "\"lastName\":\"Rao\","
                + "\"dateOfBirth\":\"1990-01-10\","
                + "\"email\":\"asha@example.com\","
                + "\"phone\":\"+14165550123\","
                + "\"addressLine1\":\"100 King Street\","
                + "\"addressLine2\":null,"
                + "\"city\":\"Toronto\","
                + "\"provinceState\":\"Ontario\","
                + "\"postalCode\":\"M5H1J9\","
                + "\"countryCode\":\"CA\","
                + "\"branchId\":" + branchId + ","
                + "\"currency\":\"CAD\""
                + "}";
    }

    private String moneyJson(String amount, String idempotencyKey) {
        return "{"
                + "\"amount\":" + amount + ","
                + "\"currency\":\"CAD\","
                + "\"description\":\"test transaction\","
                + "\"idempotencyKey\":\"" + idempotencyKey + "\""
                + "}";
    }
}
