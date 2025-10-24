package com.sti.accounting.entities;

import com.sti.accounting.models.StatusTransaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "debit_notes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DebitNotesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "TRANSACTION_ID", nullable = false)
    private TransactionEntity transaction;

    @Column(name = "DESCRIPTION_NOTE")
    private String descriptionNote;

    @ManyToOne
    @JoinColumn(name = "diary_id", referencedColumnName = "id")
    private AccountingJournalEntity accountingJournal;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private StatusTransaction status;

    @Column(name = "DATE")
    private LocalDate createAtDate;

    @CreationTimestamp
    @Column(name = "CREATION_DATE")
    private LocalDateTime createAtTime;

    @ManyToOne
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriodEntity accountingPeriod;

    @OneToMany(mappedBy = "debitNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DebitNotesDetailEntity> debitNoteDetail;

    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "CREATED_BY")
    private String createdBy;
}
