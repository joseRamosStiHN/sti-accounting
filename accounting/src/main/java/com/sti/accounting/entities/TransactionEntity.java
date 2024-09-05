package com.sti.accounting.entities;

import com.sti.accounting.models.StatusTransaction;
import com.sti.accounting.utils.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "CREATION_DATE")
    private LocalDateTime createAtTime;

    @Column(name = "DATE")
    private LocalDate createAtDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private StatusTransaction status;

    @Column(name = "REFERENCE")
    private String reference;

    @Column(name = "EXCHANGE_RATE")
    private BigDecimal exchangeRate;

    @Column(name = "DESCRIPTION_PDA")
    private String descriptionPda;

    @Column(name = "NUMBER_PDA")
    private BigInteger numberPda;

    @Enumerated(EnumType.STRING)
    @Column(name = "CURRENCY")
    private Currency currency;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionDetailEntity> transactionDetail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;


}
