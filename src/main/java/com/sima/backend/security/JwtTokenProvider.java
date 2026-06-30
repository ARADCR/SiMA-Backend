package com.sima.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera, valida y extrae información de los tokens JWT.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration; // En milisegundos (ej: 3600000 = 1 hora)

    // Generar token al hacer login
    public String generarToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .subject(String.valueOf(userDetails.getIdUsuario()))
                .claim("userId", userDetails.getIdUsuario())
                .claim("correo", userDetails.getUsername())
                .claim("nombre", userDetails.getNombre())
                .claim("rol", userDetails.getRol())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSecretKey())
                .compact();
    }

    // Obtener ID de usuario desde el token
    public Integer getIdUsuarioDesdeToken(String token) {
        Claims claims = parsearClaims(token);
        return Integer.parseInt(claims.getSubject());
    }

    // Obtener rol desde el token
    public String getRolDesdeToken(String token) {
        Claims claims = parsearClaims(token);
        return claims.get("rol", String.class);
    }

    // Obtener correo desde el token
    public String getCorreoDesdeToken(String token) {
        Claims claims = parsearClaims(token);
        return claims.get("correo", String.class);
    }

    // Validar si el token es válido (firma y expiración)
    public boolean validarToken(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("Token JWT expirado: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("Token JWT malformado: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Token JWT no soportado: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims vacío: " + e.getMessage());
        }
        return false;
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}