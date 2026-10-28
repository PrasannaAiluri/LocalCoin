package ca.localcoin.banking.repository;

import ca.localcoin.banking.domain.LegalPerson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalPersonRepository extends JpaRepository<LegalPerson, Long> { }
