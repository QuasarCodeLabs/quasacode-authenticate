FROM debian:latest
WORKDIR /work
COPY target/quasar-authenticate-1.0.0-SNAPSHOT-runner /application
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --chown=1001:root target/*-runner /work/application
RUN chmod 775 /application
EXPOSE 8080
USER 1001

ENTRYPOINT ["/application"]