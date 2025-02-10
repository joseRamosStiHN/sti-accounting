package com.sti.accounting.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyValueDto {

    public KeyValueDto(Long id, String name, String description){
        this.id = id;
        this.name = name;
        this.description = description;
    }

    private Long id;
    private String name;
    private String description;

    private boolean isGlobal;
}
