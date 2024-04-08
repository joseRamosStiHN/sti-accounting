package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sti.accounting.utils.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "transacciones")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "fecha_creacion")
    private LocalDateTime createAtTime;

    @Column(name = "fecha")
    private LocalDate createAtDate;

    @Column(name = "estado")
    private Long status;

    @Column(name = "referencia")
    private String reference;

    @Column(name = "tipo_documento")
    private BigInteger documentType;

    @Column(name = "tasa_cambio")
    private BigDecimal exchangeRate;

    @Column(name = "descripcion_pda")
    private String descriptionPda;

    @Column(name = "numero_partida")
    private BigInteger numberPda;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda")
    private Currency currency;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<TransactionDetailEntity> transactionDetail;

}
