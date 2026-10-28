package ca.localcoin.banking.repository;

import ca.localcoin.banking.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {
    // This lock prevents two withdrawals from using the same old balance at once.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from AccountBalance b where b.account.id = :accountId")
    Optional<AccountBalance> findLockedByAccountId(@Param("accountId") Long accountId);
}
