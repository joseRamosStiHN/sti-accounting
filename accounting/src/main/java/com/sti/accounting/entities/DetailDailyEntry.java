package com.sti.accounting.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailDailyEntry {

    @Id
    private Long id;

    private Long accountId;
    private Boolean isMain;
    private Boolean isActive;

}
