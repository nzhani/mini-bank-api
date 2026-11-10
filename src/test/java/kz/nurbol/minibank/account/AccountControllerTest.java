package kz.nurbol.minibank.account;

import kz.nurbol.minibank.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AccountService service;

    @Test
    void openAccountReturns201WithLocation() throws Exception {
        Account account = new Account("KZ123", "Nurbol", "KZT");
        ReflectionTestUtils.setField(account, "id", 7L);
        when(service.open("Nurbol", "KZT")).thenReturn(account);

        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Nurbol\",\"currency\":\"KZT\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/accounts/7"))
                .andExpect(jsonPath("$.accountNumber").value("KZ123"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void invalidCurrencyReturns400() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Nurbol\",\"currency\":\"tenge\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        when(service.get(99L)).thenThrow(new NotFoundException("Account 99 not found"));

        mvc.perform(get("/api/v1/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
