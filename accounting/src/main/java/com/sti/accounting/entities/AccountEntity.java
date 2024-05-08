package com.sti.accounting.entities;


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

    @Column(name = "TYPICAL_BALANCE")
    private String typicalBalance;

    @Column(name = "SUPPORTS_REGISTRATION")
    private boolean supportsRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @OneToMany(mappedBy = "account", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<BalancesEntity> balances;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private AccountCategoryEntity accountCategory;

}
