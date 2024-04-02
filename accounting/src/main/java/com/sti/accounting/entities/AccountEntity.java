package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sti.accounting.models.AccountRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "cuentas")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo")
    private String code;

    @Column(name = "descripcion")
    private String description;

    @Column(name = "tipo_estado_financiero")
    private String financialStatementType;

    @Column(name = "parent_id")
    private BigDecimal parentId;

    @Column(name = "moneda")
    private String currency;

    @Column(name = "categoria")
    private BigDecimal category;

    @Column(name = "tipo_cuenta")
    private BigDecimal accountType;

    @Column(name = "saldo_tipico")
    private String typicalBalance;

    @Column(name = "admite_registro")
    private boolean supportsRegistration;

    @Column(name = "saldo_inicial")
    private BigDecimal initialBalance;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<BalancesEntity> balances;

    //constructors
    public AccountEntity(Long id) {
        this.id = id;
    }

    public AccountRequest entityToRequest() {
        AccountRequest request = new AccountRequest();
        request.setId(this.getId());
        request.setCode(this.getCode());
        request.setDescription(this.getDescription());
        request.setFinancialStatementType(this.getFinancialStatementType());
        request.setParentId(this.getParentId());
        request.setCurrency(this.getCurrency());
        request.setCategory(this.getCategory());
        request.setAccountType(this.getAccountType());
        request.setTypicalBalance(this.getTypicalBalance());
        request.setSupportsRegistration(this.supportsRegistration);
        request.setInitialBalance(this.getInitialBalance());
        request.setBalances(this.getBalances());
        return request;
    }
}
