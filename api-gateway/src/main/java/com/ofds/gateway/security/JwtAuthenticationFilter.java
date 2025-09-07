package com.ofds.gateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();
        
        // Public routes (skip JWT check)
        if (path.startsWith("/auth")) {
            return chain.filter(exchange); // allow without token
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        System.out.println("Authorization Header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("No Bearer token found....");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        String token = authHeader.substring(7);
        System.out.println("JWT Token extracted: " + token);

        return Mono.just(token)
            .flatMap(t -> {
                try {
                    String username = jwtService.extractUsername(t);
                    System.out.println("Extracted username from token: " + username);

                    if (username == null) {
                        System.out.println("Username is null, unauthorized.");
                        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    }

                    boolean valid = jwtService.isTokenValid(t, username);
                    System.out.println("Is token valid? " + valid);

                    if (!valid) {
                        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    }

                    var claims = jwtService.extractClaims(t);
                    String roles = claims.get("roles", String.class);
                    System.out.println("Roles from token claims: " + roles);

                    List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                    if (roles != null && !roles.isEmpty()) {
                        authorities = Arrays.stream(roles.split(","))
                                .map(String::trim)
                                .map(SimpleGrantedAuthority::new)
                                .toList();
                    }

                    System.out.println("Granted Authorities: " + authorities);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    new User(username, "", authorities),
                                    null,
                                    authorities);

                    System.out.println("Setting authentication in reactive security context");
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authToken))));

                } catch (Exception e) {
                    System.out.println("Exception while processing JWT: " + e.getMessage());
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            });
    }
}
