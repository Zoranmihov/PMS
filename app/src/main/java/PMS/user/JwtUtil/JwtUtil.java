package PMS.user.JwtUtil;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.lang.Arrays;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import PMS.user.DTO.UserClaimsDTO;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    @Value("${jwt_secret}")
    private String jwtSecret;

    @Value("${jwt_expiration}")
    private int jwtExpiration;

    @Value("${jwt_refresh_expiration}")
    private int jwtRefreshExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long id, String email, String[] roles) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtExpiration))
                .claim("id", id)
                .claim("email", email)
                .claim("roles", Arrays.asList(roles))
                .signWith(key)
                .compact();

    }

    public String generateRefreshToken(Long id) {
        return Jwts.builder()
                .subject(id.toString())
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtRefreshExpiration))
                .claim("type", "refresh")
                .signWith(key)
                .compact();

    }

    public UserClaimsDTO getUserFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();

        Long id = claims.get("id", Number.class).longValue();
        String email = claims.get("email", String.class);

        String[] roles = ((java.util.List<?>) claims.get("roles"))
                .stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        return new UserClaimsDTO(id, email, roles);

    }

    public boolean validateJwt(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
