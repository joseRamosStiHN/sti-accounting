package com.sti.accounting.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sti.accounting.utils.Motion;
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
    @JsonIgnore
    private AccountEntity account;

    @Column(name = "monto")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "movimiento")
    private Motion motion;
}
