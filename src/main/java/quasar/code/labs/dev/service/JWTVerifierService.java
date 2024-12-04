package quasar.code.labs.dev.service;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.security.interfaces.RSAPublicKey;

public class JWTVerifierService {

    private RSAPublicKey publicKey;

    public JWTVerifierService(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public DecodedJWT verifyToken(String token) throws Exception {
        Algorithm algorithm = Algorithm.RSA256(publicKey, null); // Se pasa la clave pública
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("https://quasarcode.dev")
                .build();

        return verifier.verify(token);  // Verificación del token
    }
}