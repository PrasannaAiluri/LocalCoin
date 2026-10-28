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
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_legal_person_id")
    private LegalPerson actor;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;
    @Column(name = "entity_id", nullable = false, length = 80)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Outcome outcome;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    @Column(length = 1000)
    private String metadata;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditEvent() { }

    public AuditEvent(String eventType, String entityType, String entityId,
                      Outcome outcome, String correlationId, String metadata) {
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.outcome = outcome;
        this.correlationId = correlationId;
        this.metadata = metadata;
    }

    public enum Outcome { SUCCESS, FAILURE, DENIED }
}
