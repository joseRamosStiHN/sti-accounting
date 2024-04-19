package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.utils.Status;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "accounts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PARENT_ID")
    private BigDecimal parentId;

    @Column(name = "CATEGORY")
    private BigDecimal category;

    @Column(name = "TYPICAL_BALANCE")
    private String typicalBalance;

    @Column(name = "SUPPORTS_REGISTRATION")
    private boolean supportsRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

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
        request.setStatus(this.getStatus());
        request.setBalances(this.getBalances());
        return request;
    }
}
