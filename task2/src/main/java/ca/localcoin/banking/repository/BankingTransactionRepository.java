package ca.localcoin.banking.repository;

import ca.localcoin.banking.domain.BankingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface BankingTransactionRepository extends JpaRepository<BankingTransaction, Long> {
    Optional<BankingTransaction> findBySourceAccount_IdAndIdempotencyKey(Long accountId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from BankingTransaction t where t.sourceAccount.id = :accountId and t.idempotencyKey = :key")
    Optional<BankingTransaction> findLockedByAccountAndKey(@Param("accountId") Long accountId,
                                                           @Param("key") String key);
}
