package com.sti.accounting.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "control_account_balances")
@Getter
@Setter
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

}
