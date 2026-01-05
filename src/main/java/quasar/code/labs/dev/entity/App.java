package quasar.code.labs.dev.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;
import lombok.*;
import quasar.code.labs.dev.service.AppName;

import java.util.Objects;

@Entity
@Setter
@Getter
@ToString
@RegisterForReflection
@Table(name = "app", schema = "quasar_authenticate")
public class App extends PanacheEntity {

    @Column(nullable = false, updatable = true, unique = true)
    @Enumerated(EnumType.STRING)
    private AppName nameApp;


    @Column(nullable = false, updatable = true)
    private Boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof App)) return false;
        App other = (App) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
