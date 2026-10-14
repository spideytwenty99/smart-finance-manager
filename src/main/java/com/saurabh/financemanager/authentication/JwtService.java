package com.saurabh.financemanager.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirySeconds;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${jwt.issuer}") String issuer,
                      @Value("${jwt.expiry}") long expirySeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirySeconds = expirySeconds;
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .subject(principal.getId().toString())
                .claim("email", principal.getUsername())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }
}