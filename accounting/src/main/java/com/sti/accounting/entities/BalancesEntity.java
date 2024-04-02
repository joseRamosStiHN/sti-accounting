package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.models.BalancesRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saldos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BalancesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_cuenta", nullable = false)
    @JsonIgnore
    private AccountEntity account;

    @Column(name = "saldo_inicial")
    private BigDecimal initialBalance;

    @CreationTimestamp
    @Column(name = "fecha")
    private LocalDateTime createAtDate;

    @Column(name = "is_actual")
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
