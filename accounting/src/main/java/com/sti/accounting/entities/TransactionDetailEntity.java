package com.sti.accounting.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_transaccion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_transaccion", nullable = false)
    private TransactionEntity transaction;


    @ManyToOne
    @JoinColumn(name = "id_cuenta", nullable = false)
    private AccountEntity account;

    @Column(name = "monto")
    private BigDecimal amount;
}
