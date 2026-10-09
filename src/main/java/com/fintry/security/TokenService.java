package com.fintry.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final Duration ttl;
    public TokenService(JwtEncoder encoder,
            @Value("${fintry.security.jwt.issuer}") String issuer,
            @Value("${fintry.security.jwt.audience}") String audience,
            @Value("${fintry.security.jwt.ttl}") Duration ttl) {
        if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofMinutes(15)) > 0)
            throw new IllegalArgumentException("JWT lifetime must be between zero and 15 minutes");
        this.encoder = encoder; this.issuer = issuer; this.audience = audience; this.ttl = ttl;
    }
    public long expiresIn() { return ttl.toSeconds(); }
    public String issue(Long userId) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(userId.toString()).issuedAt(now).notBefore(now).expiresAt(now.plus(ttl)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
}
