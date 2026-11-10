package kz.nurbol.minibank.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Request/response records: the API never exposes JPA entities directly. */
public final class AccountDtos {

    private AccountDtos() {
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 100) String ownerName,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 code, e.g. KZT") String currency) {
    }

    public record DepositRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
    }

    public record AccountResponse(
            Long id,
            String accountNumber,
            String ownerName,
            String currency,
            BigDecimal balance,
            AccountStatus status,
            OffsetDateTime createdAt) {

        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getAccountNumber(), a.getOwnerName(),
                    a.getCurrency(), a.getBalance(), a.getStatus(), a.getCreatedAt());
        }
    }
}
