package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sti.accounting.models.BalancesRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balances")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BalancesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    @JsonIgnore
    private AccountEntity account;

    @Column(name = "INITIAL BALANCE")
    @NotNull(message = "Initial Balance is required")
    @Positive(message = "Initial Balance must be positive")
    private BigDecimal initialBalance;

    @CreationTimestamp
    @Column(name = "DATE")
    private LocalDateTime createAtDate;

    @Column(name = "IS_ACTUAL")
    private Boolean isActual;

    public BalancesRequest entityToRequest(){
        BalancesRequest request = new BalancesRequest();
        request.setId(this.getId());
        request.setInitialBalance(this.getInitialBalance());
        request.setCreateAtDate(this.getCreateAtDate());
        request.setIsActual(this.getIsActual());
        return request;
    }

}
