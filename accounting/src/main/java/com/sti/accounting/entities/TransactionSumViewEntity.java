package com.sti.accounting.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "book_account_sum")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSumViewEntity {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNT_ID")
    private String accountId;

    @Column(name = "DEBIT")
    private String debit;

    @Column(name = "CREDIT")
    private String credit;

}
