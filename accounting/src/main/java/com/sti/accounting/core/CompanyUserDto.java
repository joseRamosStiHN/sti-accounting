package com.sti.accounting.core;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompanyUserDto {

    private Long id;

    private CompanyDto company;
    private List<KeyValueDto> roles;
}
