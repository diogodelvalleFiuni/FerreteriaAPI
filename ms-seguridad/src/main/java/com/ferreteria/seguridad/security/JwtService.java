package com.ferreteria.seguridad.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite y valida los tokens.
 * <ul>
 *   <li><b>Access token</b>: JWT compacto de 3 secciones que validan ms-inventario y ms-cache-redis.
 *       Claims: {@code sub} = username, {@code roles} (nombre configurable) sin prefijo ROLE_, {@code iat}, {@code exp}
 *       e {@code iss} solo si JWT_ISSUER esta definido. Sin datos de persona.</li>
 *   <li><b>Refresh token</b>: firmado con OTRA clave (JWT_REFRESH_SECRET), sin roles, con {@code token_use=refresh}
 *       y {@code jti}. Solo sirve en {@code /api/auth/refresh}; los demas servicios lo rechazan por firma.</li>
 * </ul>
 */
@Service
public class JwtService {

    public static final String TOKEN_USE_CLAIM = "token_use";
    public static final String REFRESH_USE = "refresh";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder accessEncoder;
    private final JwtEncoder refreshEncoder;
    private final JwtDecoder refreshDecoder;
    private final JwtProperties props;
    private final MacAlgorithm algorithm;

    public JwtService(@Qualifier("jwtEncoder") JwtEncoder accessEncoder,
            @Qualifier("refreshJwtEncoder") JwtEncoder refreshEncoder,
            @Qualifier("refreshJwtDecoder") JwtDecoder refreshDecoder,
            JwtProperties props) {
        this.accessEncoder = accessEncoder;
        this.refreshEncoder = refreshEncoder;
        this.refreshDecoder = refreshDecoder;
        this.props = props;
        this.algorithm = JwtKeys.resolveAlgorithm(props.algorithm());
    }

    public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(a -> a.startsWith(ROLE_PREFIX) ? a.substring(ROLE_PREFIX.length()) : a)
                .distinct()
                .toList();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(props.expirationMinutes(), ChronoUnit.MINUTES))
                .claim(props.rolesClaim(), roles);
        if (hasIssuer()) {
            claims.issuer(props.issuer());
        }
        return encode(accessEncoder, claims.build());
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(props.refreshExpirationDays(), ChronoUnit.DAYS))
                .claim(TOKEN_USE_CLAIM, REFRESH_USE);
        if (hasIssuer()) {
            claims.issuer(props.issuer());
        }
        return encode(refreshEncoder, claims.build());
    }

    /**
     * Valida un refresh token (firma con la clave de refresh, vencimiento, emisor y {@code token_use}).
     *
     * @return el username (claim sub)
     * @throws org.springframework.security.oauth2.jwt.JwtException si el token no sirve
     */
    public String validateRefreshToken(String token) {
        Jwt jwt = refreshDecoder.decode(token);
        if (!REFRESH_USE.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM))) {
            throw new BadJwtException("El token no es un refresh token");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BadJwtException("El refresh token no tiene usuario (sub)");
        }
        return subject;
    }

    public long expiresInSeconds() {
        return props.expirationMinutes() * 60;
    }

    public long refreshExpiresInSeconds() {
        return props.refreshExpirationDays() * 24 * 60 * 60;
    }

    private boolean hasIssuer() {
        return props.issuer() != null && !props.issuer().isBlank();
    }

    private String encode(JwtEncoder encoder, JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(algorithm).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
