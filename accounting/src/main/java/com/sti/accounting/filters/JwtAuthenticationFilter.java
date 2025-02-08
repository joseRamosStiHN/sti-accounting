package com.sti.accounting.filters;

import com.sti.accounting.core.CustomUserDetails;
import com.sti.accounting.core.SecurityUserDto;
import com.sti.accounting.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
            // create custom user
            CustomUserDetails customUserDetails = new CustomUserDetails(userDetails,tenantId, authorities);
            //create SecurityContextHolder

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    customUserDetails,
                    null,
                    customUserDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (RuntimeException e) {
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
}