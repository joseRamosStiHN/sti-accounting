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
@Table(name = "credit_notes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditNotesEntity {

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

    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditNotesDetailEntity> creditNoteDetail;
}
