package com.ferreteria.ventas.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de seguridad de ms-ventas: API stateless protegida por el token que emite ms-seguridad (un refresh token
 * no sirve: va firmado con otra clave). Solo Swagger queda publico.
 * <p>
 * Dos capas de autorizacion, ambas con los roles de {@link AccessProperties}:
 * <ul>
 *     <li>por URL (aca): lectura / escritura / borrado segun metodo HTTP</li>
 *     <li>por metodo: {@code @PreAuthorize} en {@code AbstractBaseController} (@EnableMethodSecurity)</li>
 * </ul>
 * La busqueda es {@code POST /api/{recurso}/search}: aunque use POST es una LECTURA, por eso se declara antes que
 * la regla de escritura.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AccessProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtProperties properties,
            RestSecurityErrorHandler errorHandler, AccessProperties access) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole(access.deleteRoles())
                        .requestMatchers(HttpMethod.POST, "/api/*/search").hasAnyRole(access.readRoles())
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(access.readRoles())
                        .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole(access.writeRoles())
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole(access.writeRoles())
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .addFilterBefore(new AuthFilter(jwtDecoder, properties, errorHandler),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
