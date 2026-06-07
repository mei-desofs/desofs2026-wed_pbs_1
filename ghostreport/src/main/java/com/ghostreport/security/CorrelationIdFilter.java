package com.ghostreport.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CorrelationId.set(request.getHeader(CorrelationId.HEADER));
        response.setHeader(CorrelationId.HEADER, CorrelationId.current());
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationId.clear();
        }
    }
}
