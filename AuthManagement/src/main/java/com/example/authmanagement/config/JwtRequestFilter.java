package com.example.authmanagement.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.example.authmanagement.entities.Credential;
import com.example.authmanagement.repositories.CredentialRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.authmanagement.services.JwtUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CredentialRepository credentialRepository;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/validate",
            "/auth/sync",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs",
            "/swagger-resources",
            "/webjars",
            "/api1/v3/api-docs",
            "/api2/v3/api-docs"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestPath = request.getRequestURI();
        final String requestTokenHeader = request.getHeader("Authorization");

        if (isPublicPath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }


        UUID userId = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                userId = jwtTokenUtil.extractUserId(jwtToken);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired");
            } catch (Exception e) {
                logger.warn("Unable to get JWT Token or extract userId: " + e.getMessage());
            }
        } else if (!requestPath.contains("swagger") && !requestPath.contains("api-docs")){
            logger.warn("JWT Token does not begin with Bearer String");
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            Credential credential = credentialRepository.findByUserId(userId)
                    .orElse(null);

            if (credential != null && jwtTokenUtil.isTokenValid(jwtToken)) {

                UserDetails userDetails = new User(
                        credential.getUsername(),
                        credential.getPassword(),
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + credential.getRole()))
                );

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String requestPath) {
        if (requestPath.contains("swagger") || requestPath.contains("api-docs") ||
                requestPath.contains("swagger-ui") || requestPath.contains("swagger-resources") ||
                requestPath.contains("webjars")) {
            return true;
        }

        return PUBLIC_PATHS.stream().anyMatch(requestPath::contains);
    }
}