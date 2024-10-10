package com.sti.accounting.models;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CreditNotesResponse {

    private Long id;

    private Long transactionId;

    private String descriptionNote;

    private Long diaryType;

    private String diaryName;

    private String status;

    private LocalDateTime creationDate;

    private LocalDate date;

    private Set<CreditNotesDetailResponse> detailNote;
}
