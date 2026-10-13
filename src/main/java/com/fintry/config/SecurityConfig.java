package com.fintry.config;

import com.fintry.repository.UserRepository;
import com.fintry.security.ApiSecurityErrors;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean RSAKey signingKey(@Value("${fintry.security.jwt.private-key}") String privateKey,
            @Value("${fintry.security.jwt.public-key}") String publicKey) throws Exception {
        var factory = KeyFactory.getInstance("RSA");
        var pub = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
        var priv = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey)));
        if (pub.getModulus().bitLength() < 2048 || !pub.getModulus().equals(priv.getModulus()))
            throw new IllegalArgumentException("Matching RSA keys of at least 2048 bits are required");
        return new RSAKey.Builder(pub).privateKey(priv).build();
    }
    @Bean JwtEncoder jwtEncoder(RSAKey key) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
    @Bean JwtDecoder jwtDecoder(RSAKey key,
            @Value("${fintry.security.jwt.issuer}") String issuer,
            @Value("${fintry.security.jwt.audience}") String audience) throws Exception {
        var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).signatureAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> required = jwt -> {
            try {
                long id = Long.parseLong(jwt.getSubject());
                if (id <= 0 || jwt.getExpiresAt() == null || jwt.getIssuedAt() == null || !jwt.getAudience().contains(audience))
                    throw new IllegalArgumentException();
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ex) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token claims", null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), required));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, UserRepository users, @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Only explicit Bearer credentials; no authentication cookies.
            .cors(c -> c.configurationSource(cors))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/users", "/accounts/user/**").denyAll()
                .requestMatchers(HttpMethod.GET, "/users", "/admin/balance-requests").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/instruments").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/instruments/*/price", "/balance-requests/*/approve").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/auth/me", "/accounts/me", "/accounts/user/*", "/portfolio/me", "/portfolio/user/*",
                    "/transactions/me", "/transactions/user/*", "/instruments", "/balance-requests/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/transactions/buy", "/transactions/sell", "/balance-requests").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> ApiSecurityErrors.write(res, HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> ApiSecurityErrors.write(res, HttpStatus.FORBIDDEN)))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint((req, res, ex) -> ApiSecurityErrors.write(res, HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> ApiSecurityErrors.write(res, HttpStatus.FORBIDDEN))
                .jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                    var user = users.findById(Long.valueOf(jwt.getSubject()))
                        .filter(u -> u.isEnabled() && u.getPasswordHash() != null)
                        .orElseThrow(() -> new InvalidBearerTokenException("Inactive identity"));
                    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), user.getId().toString());
                })));
        return http.build();
    }
}
