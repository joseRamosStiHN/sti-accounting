package com.sti.accounting.entities;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balances")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalancesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private AccountEntity account;

    @Column(name = "TYPICAL_BALANCE")
    private String typicalBalance;

    @Column(name = "INITIAL BALANCE")
    private BigDecimal initialBalance;

    @CreationTimestamp
    @Column(name = "DATE")
    private LocalDateTime createAtDate;

    @Column(name = "IS_CURRENT")
    private Boolean isCurrent;

    @Column(name = "TENANT_ID")
    private String tenantId;
}
