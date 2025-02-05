package com.sti.accounting.entities;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(name = "balance_general")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionBalanceGeneralEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "ACCOUNTNAME")
    private String accountName;

    @Column(name = "PARENTID")
    private Integer parentId;

    @Column(name = "CATEGORY")
    private String category;

    @Column(name = "ISROOT")
    private boolean isRoot;

    @Column(name = "DEBIT")
    private BigDecimal debit;

    @Column(name = "CREDIT")
    private BigDecimal credit;



}
