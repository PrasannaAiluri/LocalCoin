package ca.localcoin.banking.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "legal_person")
public class LegalPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legal_person_id")
    private Long id;


    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name = "middle_name", length = 100)
    private String middleName;
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    @Column(length = 254)
    private String email;
    @Column(name = "phone_e164", length = 20)
    private String phone;

    // Store a government ID only after encrypting it with a managed key.
    // This project deliberately does not accept or log raw government ID values.
    @Column(name = "government_id_type", length = 40)
    private String governmentIdType;
    @Column(name = "government_id_value_encrypted", length = 512)
    private byte[] governmentIdValueEncrypted;
    @Column(name = "government_id_last4", length = 4)
    private String governmentIdLast4;

    @Column(name = "identity_verified", nullable = false)
    private boolean identityVerified = false;
    @Column(name = "identity_verified_at")
    private Instant identityVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PersonStatus status = PersonStatus.ACTIVE;

    protected LegalPerson() { }

    public LegalPerson(String firstName, String middleName, String lastName,
                       LocalDate dateOfBirth, String email, String phone) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public enum PersonStatus { ACTIVE, RESTRICTED, CLOSED }
}
