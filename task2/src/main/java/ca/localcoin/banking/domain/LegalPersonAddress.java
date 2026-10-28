package ca.localcoin.banking.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "legal_person_address")
public class LegalPersonAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_person_id", nullable = false)
    private LegalPerson legalPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 15)
    private AddressType addressType = AddressType.RESIDENTIAL;
    @Column(name = "address_line1", nullable = false, length = 160)
    private String addressLine1;
    @Column(name = "address_line2", length = 160)
    private String addressLine2;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(name = "province_state", length = 80)
    private String provinceState;
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;
    @Column(name = "valid_to")
    private LocalDate validTo;
    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    protected LegalPersonAddress() { }

    public LegalPersonAddress(LegalPerson person, String line1, String line2,
                              String city, String provinceState, String postalCode,
                              String countryCode) {
        this.legalPerson = person;
        this.addressLine1 = line1;
        this.addressLine2 = line2;
        this.city = city;
        this.provinceState = provinceState;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.validFrom = LocalDate.now();
    }

    public enum AddressType { RESIDENTIAL, MAILING, OTHER }
}
