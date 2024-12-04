package quasar.code.labs.dev.service;

import quasar.code.labs.dev.exceptions.user.UserException;

import java.io.InputStream;
import java.util.Properties;

public class EmailServiceConfig {
    public static Properties loadEmailConfig() {
        Properties properties = new Properties();
        try (InputStream input = EmailServiceConfig.class.getClassLoader().getResourceAsStream("email.properties")) {
            if (input == null) {
                throw new UserException("No se encontró el archivo email.properties");
            }
            properties.load(input);
        } catch (Exception e) {
            throw new UserException("Error al cargar configuración de email" + e.getMessage());
        }
        return properties;
    }
}

