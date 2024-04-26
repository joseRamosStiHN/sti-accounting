package com.sti.accounting.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "book_account")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionViewEntity {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNT_ID")
    private String account_id;
    @Column(name = "DESCRIPCION")
    private String descripcion;
    @Column(name = "REFERENCE")
    private String reference;
    @Column(name = "DOCUMENT_TYPE")
    private String document_type;
    @Column(name = "EXCHANGE_RATE")
    private String exchange_rate;
    @Column(name = "CURRENCY")
    private String currency;
    @Column(name = "ACCOUNT_NAME")
    private String account_name;
    @Column(name = "ACCOUNT_ID_PDA")
    private String account_id_pda;
    @Column(name = "ACCOUNT_NAME_PDA")
    private String account_name_pda;
    @Column(name = "AMOUNT")
    private String amount;
    @Column(name = "MOTION")
    private String motion;
    @Column(name = "DEBIT")
    private String debit;
    @Column(name = "CREDIT")
    private String credit;


}
