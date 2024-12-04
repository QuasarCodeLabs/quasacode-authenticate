package quasar.code.labs.dev.utils;

import quasar.code.labs.dev.exceptions.user.UserException;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyLoader {
    public RSAPublicKey loadPublicKey() throws UserException, NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        byte[] keyBytes;
        try (InputStream publicKeyStream = getClass().getClassLoader().getResourceAsStream("META-INF/resources/publicKey.pem")) {

            if (publicKeyStream == null) {
                throw new UserException("Public key not found");
            }

            keyBytes = publicKeyStream.readAllBytes();
        }
        String publicKeyContent = new String(keyBytes).replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s+", "").trim();
        byte[] decoded = Base64.getDecoder().decode(publicKeyContent);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    public RSAPrivateKey loadPrivateKey() throws UserException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes;
        try (InputStream privateKeyStream = getClass().getClassLoader().getResourceAsStream("META-INF/resources/privateKey.pem")) {
            if (privateKeyStream == null) {
                throw new UserException("Private key not found");
            }

            keyBytes = privateKeyStream.readAllBytes();
        }
        String privateKeyContent = new String(keyBytes).replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s+", "").trim();
        byte[] decoded = Base64.getDecoder().decode(privateKeyContent);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }

}
