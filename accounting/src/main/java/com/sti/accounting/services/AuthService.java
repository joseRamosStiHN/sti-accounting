package com.sti.accounting.services;

import com.sti.accounting.core.CustomUserDetails;
import com.sti.accounting.core.KeyValueDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    public CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }

        throw new RuntimeException("Usuario no autenticado o no válido");
    }

    public String getUsername() {
        return getAuthenticatedUser().getUser().getUserName();
    }

    public Long getUserId() {
        return getAuthenticatedUser().getUser().getId();
    }

    public String getTenantId() {
        return getAuthenticatedUser().getTenantId();
    }

    public List<KeyValueDto> getUserRoles() {
        return getAuthenticatedUser().getUser().getGlobalRoles();
    }


    //Actualmente los roles no estan validando las apis cuando se tenga esa funcionalidad anadirla
//    public List<KeyValueDto> getCurrentCompanyRole() {
//        String tenantId = getTenantId();
//        return getAuthenticatedUser().getUser().getCompanie().
//     //           .filter(f->f.getCompany().getTenantId().equals(tenantId)
//    }

    public boolean hasRole(String roleName) {
        return getAuthenticatedUser().getUser().getGlobalRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
}
