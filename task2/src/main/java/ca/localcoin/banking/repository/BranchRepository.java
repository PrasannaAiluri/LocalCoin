package ca.localcoin.banking.repository;

import ca.localcoin.banking.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> { }
