package com.sti.accounting.entities;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "control_account_balances")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ControlAccountBalancesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    @Column(name = "DEBIT")
    private String debit;

    @Column(name = "CREDIT")
    private String credit;

    @ManyToOne
    @JoinColumn(name = "ACCOUNTING_PERIOD_ID", nullable = false)
    private AccountingPeriodEntity accountingPeriod;
}
