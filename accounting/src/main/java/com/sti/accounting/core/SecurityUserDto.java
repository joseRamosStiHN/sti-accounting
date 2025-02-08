package com.sti.accounting.core;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SecurityUserDto {
    private Long id;
    private String userName;
    private String firstName;
    private String lastName;
    private String userAddress;
    private String userPhone;
    private String email;
    private LocalDateTime createdAt;
    private boolean isActive;
    private List<KeyValueDto> globalRoles;
    private List<CompanyUserDto> companies;
}
