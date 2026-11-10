package kz.nurbol.minibank.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select t from Transaction t
            where t.fromAccountId = :accountId or t.toAccountId = :accountId
            order by t.createdAt desc, t.id desc
            """)
    Page<Transaction> findHistory(@Param("accountId") Long accountId, Pageable pageable);
}
