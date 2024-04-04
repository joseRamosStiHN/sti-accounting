package com.sti.accounting.models;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class GeneralResponse {

    private Long code;
    private String description;
    private Object data;
    private List<GeneralError> errors = new ArrayList<>();

}
