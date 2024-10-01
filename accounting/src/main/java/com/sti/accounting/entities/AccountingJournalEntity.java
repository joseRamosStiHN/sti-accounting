package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_journal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountingJournalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "DIARY_NAME")
    private String diaryName;

    @ManyToOne
    @JoinColumn(name = "account_type_id", referencedColumnName = "id")
    private AccountTypeEntity accountType;

    @ManyToOne
    @JoinColumn(name = "DEFAULT_INCOME_ACCOUNT")
    @JsonIgnore
    private AccountEntity defaultIncomeAccount;

    @ManyToOne
    @JoinColumn(name = "DEFAULT_EXPENSE_ACCOUNT")
    @JsonIgnore
    private AccountEntity defaultExpenseAccount;

    @ManyToOne
    @JoinColumn(name = "CASH_ACCOUNT")
    @JsonIgnore
    private AccountEntity cashAccount;

    @ManyToOne
    @JoinColumn(name = "LOST_ACCOUNT")
    @JsonIgnore
    private AccountEntity lossAccount;

    @ManyToOne
    @JoinColumn(name = "TRANSIT_ACCOUNT")
    @JsonIgnore
    private AccountEntity transitAccount;

    @ManyToOne
    @JoinColumn(name = "PROFIT_ACCOUNT")
    @JsonIgnore
    private AccountEntity profitAccount;

    @ManyToOne
    @JoinColumn(name = "BANK_ACCOUNT")
    @JsonIgnore
    private AccountEntity bankAccount;

    @Column(name = "ACCOUNT_NUMBER")
    private BigDecimal accountNumber;

    @ManyToOne
    @JoinColumn(name = "DEFAULT_ACCOUNT")
    @JsonIgnore
    private AccountEntity defaultAccount;

    @Column(name = "CODE")
    private String code;

    @Column(name = "STATUS")
    private boolean status;

    @CreationTimestamp
    @Column(name = "CREATION_DATE")
    private LocalDateTime createDate;
}
