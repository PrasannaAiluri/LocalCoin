package ca.localcoin.banking.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "branch")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long id;

    @Column(name = "branch_code", nullable = false, unique = true, length = 20)
    private String branchCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "address_line1", nullable = false, length = 160)
    private String addressLine1;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BranchStatus status = BranchStatus.ACTIVE;

    protected Branch() { }

    public Branch(String branchCode, String name, String addressLine1,
                  String city, String postalCode, String countryCode) {
        this.branchCode = branchCode;
        this.name = name;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
    }

    public Long getId() { return id; }
    public String getBranchCode() { return branchCode; }
    public String getName() { return name; }
    public BranchStatus getStatus() { return status; }

    public enum BranchStatus { ACTIVE, INACTIVE }
}
