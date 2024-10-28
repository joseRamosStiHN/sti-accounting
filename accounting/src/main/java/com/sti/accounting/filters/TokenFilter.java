package com.sti.accounting.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class TokenFilter implements Filter {

    public static final String BEARER = "Bearer ";
    public static final String API_SECURITY = "http://api.stiglobals.com/api.security/v1/access/sti/verify/full";

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String authHeader = request.getHeader("Authorization");

        LOGGER.info("Header : {} ", authHeader);

        if (authHeader == null || !authHeader.startsWith(BEARER)) {

            LOGGER.error("Invalid Authorization Header");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            return;

        }

        String path = request.getRequestURI();

        String[] pathParts = path.split("/");
        String tenant = "";

        if (pathParts.length > 3) {

            tenant = pathParts[3];

        }

        String apiSecurity = API_SECURITY;

        HttpURLConnection conn = null;

        try {

            URI uri = URI.create(apiSecurity);
            URL url = uri.toURL();

            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", authHeader);

            int responseCode = conn.getResponseCode();

            switch (responseCode) {

                case HttpURLConnection.HTTP_ACCEPTED -> {

                    LOGGER.info("Authorization accepted");

                    filterChain.doFilter(request, response);

                }

                case HttpURLConnection.HTTP_UNAUTHORIZED -> {

                    LOGGER.info("Authorization failed with 401 Unauthorized.");

                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                }

                default -> {

                    LOGGER.warn("Unexpected response code: " + responseCode);

                    response.setStatus(responseCode);

                }

            }

        } catch (Exception e) {

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);


        } finally {

            if (conn != null) {

                conn.disconnect();

            }

        }

    }

}