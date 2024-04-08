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

    @Column(name = "parent_id")
    private BigDecimal parentId;

    @Column(name = "categoria")
    private BigDecimal category;

    @Column(name = "saldo_tipico")
    private String typicalBalance;

    @Column(name = "admite_registro")
    private boolean supportsRegistration;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<BalancesEntity> balances;

    public AccountRequest entityToRequest() {
        AccountRequest request = new AccountRequest();
        request.setId(this.getId());
        request.setCode(this.getCode());
        request.setDescription(this.getDescription());
        request.setParentId(this.getParentId());
        request.setCategory(this.getCategory());
        request.setTypicalBalance(this.getTypicalBalance());
        request.setSupportsRegistration(this.supportsRegistration);
        request.setBalances(this.getBalances());
        return request;
    }
}
