package ca.localcoin.banking.repository;

import ca.localcoin.banking.domain.LegalPersonAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalPersonAddressRepository extends JpaRepository<LegalPersonAddress, Long> { }
