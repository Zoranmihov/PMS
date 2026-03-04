package PMS.user.Util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import PMS.user.Exceptions.ApiException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class DeactivationUtil {

    @Value("${deactivation.secret}")
    private String deactivationSecret;

    @Value("${deactivation.expiration}")
    private int deactivationExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(deactivationSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateDeactivationToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + deactivationExpiration))
                .claim("type", "deactivation")
                .signWith(key)
                .compact();

    }

    public String validateAndGetDeactivationEmail(String token) {
        var jwt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);

        String type = jwt.getPayload().get("type", String.class);
        if (!"deactivation".equals(type)) {
            throw new ApiException(403, "Invalid token");
        }

        return jwt.getPayload().getSubject();
    }
}
