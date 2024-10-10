package com.sti.accounting.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DebitNotesRequest {

    private Long id;

    private Long transactionId;

    @NotNull(message = "Description Note is required")
    private String descriptionNote;

    @NotNull(message = "Diary Type is required")
    private Long diaryType;

    @NotNull(message = "Date is required")
    private LocalDate createAtDate;

    private List<DebitNotesDetailRequest> detailNote;
}
