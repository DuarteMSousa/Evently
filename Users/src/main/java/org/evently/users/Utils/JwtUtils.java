//package org.evently.users.Utils;
//
//import io.jsonwebtoken.JwtException;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//import java.security.Key;
//
//@Component
//public class JwtUtils {
//
//    // Garante que isto não vai para o controlo de versão; guarda como variável de ambiente!
//    private final String jwtSecret = "algumasecretamuitocompridaquesegura1234567890";
//    private final long jwtExpirationMs = 86400000; // 1 dia
//
//    private Key getSigningKey() {
//        // Usa uma chave suficientemente longa para HS256
//        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
//    }
//
//    public String generateJwt(String subject) {
//        return Jwts.builder()
//                .setSubject(subject)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
//                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    public String getSubject(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();
//    }
//
//    public boolean validateJwtToken(String authToken) {
//        try {
//            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
//            return true;
//        } catch (JwtException | IllegalArgumentException e) {}
//        return false;
//    }
//}