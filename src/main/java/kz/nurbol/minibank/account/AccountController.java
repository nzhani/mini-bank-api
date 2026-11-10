package kz.nurbol.minibank.account;

import jakarta.validation.Valid;
import kz.nurbol.minibank.account.AccountDtos.AccountResponse;
import kz.nurbol.minibank.account.AccountDtos.CreateAccountRequest;
import kz.nurbol.minibank.account.AccountDtos.DepositRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody CreateAccountRequest request) {
        Account account = service.open(request.ownerName(), request.currency());
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.getId()))
                .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(service.get(id));
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        return AccountResponse.from(service.deposit(id, request.amount()));
    }

    @PostMapping("/{id}/block")
    public AccountResponse block(@PathVariable Long id) {
        return AccountResponse.from(service.block(id));
    }
}
