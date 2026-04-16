package quasar.code.labs.dev.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class EmailService {
    public static final String FROM_EMAIL = "auth@quasarcode.dev";

    @Inject
    ReactiveMailer reactiveMailer;

    public Uni<Void> sendVerificationEmail(String to, String subject, String body) {
        log.info("Enviando email reactivo a: {}", to);
        Mail mail = Mail.withHtml(to, subject, body);
        return reactiveMailer.send(mail);
    }
}
