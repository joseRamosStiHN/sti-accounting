package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sti.accounting.utils.Motion;
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

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOTION")
    private Motion motion;
}
