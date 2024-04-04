package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
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

    @CreatedDate
    @Column(name = "fecha")
    private LocalDate createAtDate;

    @Column(name = "estado")
    private Long status;

    @Column(name = "referencia")
    private String reference;

    @Column(name = "tipo_documento")
    private Long documentType;

    @Column(name = "tasa_cambio")
    private BigDecimal exchangeRate;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<TransactionDetailEntity> transactionDetail;

}
