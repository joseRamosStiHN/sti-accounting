package com.sti.accounting.filters;

import com.sti.accounting.core.CompanyDto;
import com.sti.accounting.core.CompanyUserDto;
import com.sti.accounting.core.CustomUserDetails;
import com.sti.accounting.core.SecurityUserDto;
import com.sti.accounting.entities.CompanyEntity;
import com.sti.accounting.repositories.ICompanyRepository;
import com.sti.accounting.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final ICompanyRepository companyRepository;

    public JwtAuthenticationFilter(JwtService jwtService, ICompanyRepository companyRepository) {
        this.jwtService = jwtService;
        this.companyRepository = companyRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String tenantId = request.getHeader("Tenantid");
        String token = extractTokenFromCookie(request);
        // tenantId not exist return with unauthorized
        if (tenantId == null || tenantId.isEmpty()) {
            logger.info("tenantId not exist");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Error el TenantId no encontrado\"}");
            response.getWriter().flush();
            response.getWriter().close();
            return;
        }
        // if token is null or empty return with unauthorized
        if (token == null || token.isEmpty()) {
            logger.info("token not exist");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Error el token no proved\"}");
            response.getWriter().flush();
            response.getWriter().close();
            return;
        }
        // set tenantId to context TODO: revisar esto
       // TenantContext.setCurrentTenant(tenantId);

        // validate token
        if (!jwtService.isTokenValid(token)) {
            logger.info("token expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Error el token a expirado\"}");
            response.getWriter().flush();
            response.getWriter().close();
            return;
        }


        try{
            SecurityUserDto userDetails = jwtService.getUserDetails(token);

            // Global roles of user
            List<SimpleGrantedAuthority> authorities = userDetails.getGlobalRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .toList();

            //To Do obtener el query para obtener la compania actual

            CompanyDto company = this.getCompanyByTennatId(tenantId);

            if (company== null){
                logger.info("no company valid");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Error not found Company\"}");
                response.getWriter().flush();
                response.getWriter().close();
                return;
            }

            userDetails.setCompanie(company);
            // create custom user
            CustomUserDetails customUserDetails = new CustomUserDetails(userDetails,tenantId, authorities);
            //create SecurityContextHolder

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    customUserDetails,
                    null,
                    customUserDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }


        catch (RuntimeException e) {
            logger.error("Error al autenticar usuario desde token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Error al autenticar usuario desde token\"}");
            response.getWriter().flush();
            response.getWriter().close();
            return;
        }

        filterChain.doFilter(request, response);

    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("x-auth".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public CompanyDto getCompanyByTennatId(String tennatId) {

        CompanyEntity companyEntity = companyRepository.findByTenantId(tennatId);
        CompanyDto companyDto= null;
        if (companyEntity != null){
            companyDto = new CompanyDto();
            companyDto.setId(companyEntity.getId());
            companyDto.setName(companyEntity.getCompanyName());
            companyDto.setDescription(companyEntity.getCompanyDescription());
            companyDto.setTenantId(companyEntity.getTenantId());
            companyDto.setActive(companyEntity.getIsActive());
            companyDto.setEmail(companyEntity.getCompanyEmail());
            companyDto.setCreatedAt(LocalDate.from(companyEntity.getCreatedAt()));
            companyDto.setPhone(companyEntity.getCompanyPhone());
            companyDto.setRtn(companyEntity.getCompanyRTN());
            companyDto.setWebsite(companyEntity.getCompanyWebsite());
            companyDto.setType(String.valueOf(companyEntity.getType()));

        }
        return companyDto;
    }
}