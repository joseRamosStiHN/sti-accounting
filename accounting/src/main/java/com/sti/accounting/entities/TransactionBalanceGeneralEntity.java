package com.sti.accounting.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Entity
@Table(name = "balance_general")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionBalanceGeneralEntity {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNTNAME")
    private String account_name;

    @Column(name = "PARENTID")
    private Long parentId;

    @Column(name = "CATEGORY")
    private Long category;

    @Column(name = "ISROOT")
    private boolean isRoot;

    @Column(name = "DEBIT")
    private BigDecimal debit;

    @Column(name = "CREDIT")
    private BigDecimal credit;



}
