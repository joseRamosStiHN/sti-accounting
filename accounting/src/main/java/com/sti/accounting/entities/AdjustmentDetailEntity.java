package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "adjustment_detail")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdjustmentDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ADJUSTMENT_ID", nullable = false)
    private AccountingAdjustmentsEntity adjustment;

    @ManyToOne
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    @JsonIgnore
    private AccountEntity account;

    @Column(name = "DEBIT")
    private BigDecimal debit;

    @Column(name = "CREDIT")
    private BigDecimal credit;
}
