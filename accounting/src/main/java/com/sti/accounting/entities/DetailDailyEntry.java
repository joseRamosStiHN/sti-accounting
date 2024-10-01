package com.sti.accounting.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailDailyEntry {

    @Id
    private Long id;
    private Long accountId;
    private Boolean isMain;
    private Boolean isActive;

}
