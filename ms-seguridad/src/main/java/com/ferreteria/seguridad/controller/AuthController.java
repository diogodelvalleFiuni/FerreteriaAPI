package com.ferreteria.seguridad.controller;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ferreteria.seguridad.dto.LoginRequest;
import com.ferreteria.seguridad.dto.LoginResponse;
import com.ferreteria.seguridad.dto.MeResponse;
import com.ferreteria.seguridad.dto.RefreshRequest;
import com.ferreteria.seguridad.dto.RefreshResponse;
import com.ferreteria.seguridad.security.JwtService;
import com.ferreteria.seguridad.security.LoginAttemptService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final LoginAttemptService loginAttempts;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            UserDetailsService userDetailsService, LoginAttemptService loginAttempts) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.loginAttempts = loginAttempts;
    }

    /**
     * Credenciales invalidas / usuario inactivo / usuario inexistente: siempre el mismo 401 (GlobalExceptionHandler).
     * Tras demasiados fallos de la misma IP + usuario responde 429 con Retry-After.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String key = http.getRemoteAddr() + "|" + request.username().trim().toLowerCase(Locale.ROOT);
        loginAttempts.checkAllowed(key);

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (InternalAuthenticationServiceException ex) {
            throw ex; // falla interna (ej. base caida): no es un intento fallido del cliente
        } catch (AuthenticationException ex) {
            loginAttempts.recordFailure(key);
            throw ex;
        }
        loginAttempts.recordSuccess(key);

        String token = jwtService.generateToken(auth.getName(), auth.getAuthorities());
        String refresh = jwtService.generateRefreshToken(auth.getName());
        return noStore(new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(), refresh,
                jwtService.refreshExpiresInSeconds()));
    }

    /**
     * Cambia un refresh token valido por un access token nuevo. El usuario y sus roles se vuelven a leer de la
     * base: si lo deshabilitaron o le cambiaron los roles, el access token nuevo ya lo refleja (o se rechaza).
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String username;
        try {
            username = jwtService.validateRefreshToken(request.refreshToken().trim());
        } catch (JwtException ex) {
            log.warn("401 refresh rechazado: {}", ex.getMessage());
            throw new BadCredentialsException("Refresh token invalido");
        }
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new BadCredentialsException("Usuario del refresh token inexistente");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Usuario deshabilitado");
        }
        String token = jwtService.generateToken(user.getUsername(), user.getAuthorities());
        return noStore(new RefreshResponse(token, "Bearer", jwtService.expiresInSeconds()));
    }

    /** Quien es el dueno del access token (leido de la base por AuthFilter, roles sin prefijo ROLE_). */
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith(ROLE_PREFIX) ? a.substring(ROLE_PREFIX.length()) : a)
                .sorted()
                .toList();
        return new MeResponse(authentication.getName(), roles);
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(body);
    }
}
