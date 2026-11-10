package kz.nurbol.minibank.transfer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }

    public record TransactionResponse(
            Long id, Long fromAccountId, Long toAccountId, BigDecimal amount,
            TransactionType type, OffsetDateTime createdAt) {

        static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId(), t.getFromAccountId(), t.getToAccountId(),
                    t.getAmount(), t.getType(), t.getCreatedAt());
        }
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        Transaction tx = service.transfer(request.fromAccountId(), request.toAccountId(),
                request.amount(), idempotencyKey);
        return TransactionResponse.from(tx);
    }

    @GetMapping("/accounts/{id}/transactions")
    public Page<TransactionResponse> history(@PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return service.history(id, PageRequest.of(Math.max(page, 0), safeSize))
                .map(TransactionResponse::from);
    }
}
