package com.ferreteria.seguridad.security;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Codificadores y decodificadores JWT (firma HMAC). Hay dos juegos con claves distintas:
 * <ul>
 *   <li>access ({@code jwtEncoder} / {@code jwtDecoder}): clave compartida con ms-inventario y ms-cache-redis;</li>
 *   <li>refresh ({@code refreshJwtEncoder} / {@code refreshJwtDecoder}): clave que solo conoce este servicio.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AccessProperties.class, LoginLimitProperties.class,
        BootstrapProperties.class})
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties props) {
        var algorithm = JwtKeys.resolveAlgorithm(props.algorithm());
        return new NimbusJwtEncoder(new ImmutableSecret<>(JwtKeys.secretKey(props, algorithm)));
    }

    @Bean
    public JwtEncoder refreshJwtEncoder(JwtProperties props) {
        var algorithm = JwtKeys.resolveAlgorithm(props.algorithm());
        return new NimbusJwtEncoder(new ImmutableSecret<>(JwtKeys.refreshSecretKey(props, algorithm)));
    }

    /** Valida access token (firma, exp/nbf y emisor si se configuro). Lo usa AuthFilter de este servicio. */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties props) {
        var algorithm = JwtKeys.resolveAlgorithm(props.algorithm());
        return decoder(JwtKeys.secretKey(props, algorithm), algorithm, props);
    }

    /** Valida refresh token con SU clave: un access token no pasa por aca (firma distinta). */
    @Bean
    public JwtDecoder refreshJwtDecoder(JwtProperties props) {
        var algorithm = JwtKeys.resolveAlgorithm(props.algorithm());
        return decoder(JwtKeys.refreshSecretKey(props, algorithm), algorithm, props);
    }

    private static JwtDecoder decoder(SecretKey key, MacAlgorithm algorithm, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(algorithm).build();
        OAuth2TokenValidator<Jwt> validator = props.issuer() == null || props.issuer().isBlank()
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(props.issuer());
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
