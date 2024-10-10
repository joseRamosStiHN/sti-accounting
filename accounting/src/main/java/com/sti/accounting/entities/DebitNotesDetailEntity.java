package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sti.accounting.utils.Motion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "debit_notes_detail")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DebitNotesDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DEBIT_NOTE_ID", nullable = false)
    private DebitNotesEntity debitNote;

    @ManyToOne
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    @JsonIgnore
    private AccountEntity account;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOTION")
    private Motion motion;
}
