package com.ecoanalytics.infraestructura.seguridad;

import com.ecoanalytics.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Emite el token JWT de una sesión. */
@Service
public class TokenService {

    private final JwtEncoder codificador;
    private final Duration vigencia;

    public TokenService(JwtEncoder codificador, @Value("${ecoanalytics.jwt.validity-hours}") long horasVigencia) {
        this.codificador = codificador;
        this.vigencia = Duration.ofHours(horasVigencia);
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ecoanalytics")
                .subject(usuario.getCorreo())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .claim("nombre", usuario.getNombreCompleto())
                .claim("roles", List.of(usuario.getRol().name()))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
