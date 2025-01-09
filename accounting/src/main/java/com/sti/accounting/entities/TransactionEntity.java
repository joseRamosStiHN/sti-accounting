package com.sti.accounting.entities;

import com.sti.accounting.models.StatusTransaction;
import com.sti.accounting.utils.Currency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "transactions")
@Data
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

    @Column(name = "TYPE_SALE")
    private String typeSale;

    @Column(name = "CASH_VALUE")
    private BigDecimal cashValue;

    @Column(name = "CREDIT_VALUE")
    private BigDecimal creditValue;

    @Column(name = "TYPE_PAYMENT")
    private String typePayment;

    @Column(name = "RTN")
    private String rtn;

    @Column(name = "SUPPLIER_NAME")
    private String supplierName;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionDetailEntity> transactionDetail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @ManyToOne
    @JoinColumn(name = "diary_id", referencedColumnName = "id")
    private AccountingJournalEntity accountingJournal;

    @ManyToOne
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriodEntity accountingPeriod;

    @Column(name = "TENANT_ID")
    private String tenantId;
}
