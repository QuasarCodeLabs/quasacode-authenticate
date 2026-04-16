package quasar.code.labs.dev.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
@ToString
@RegisterForReflection
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", schema = "quasar_authenticate")
public class User extends PanacheEntityBase {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 16)
    public UUID id;

    @NotBlank(message = "{person.name.notnull}")
    @Column(unique = true, nullable = false, insertable = true)
    private String username;

    @Column(nullable = false, unique = true, insertable = true)
    @NotBlank(message = "{email_empty}")
    @Email(message = "{email_invalid}")
    private String email;

    @NotNull
    @Column(nullable = false, insertable = true)
    @Size(min = 6, max = 72, message = "{length_password}")
    @NotBlank(message = "{password_empty}")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$", message = "{pattern_password}")
    private String password;

    @NotNull()
    @Column(nullable = false, insertable = true)
    private String role;

    @NotNull()
    @Column(nullable = false, insertable = true)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_app",
            schema = "quasar_authenticate",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "app_id", nullable = false),
            uniqueConstraints = {
                    @UniqueConstraint(columnNames = {"user_id", "app_id"})
            }
    )
    @NotEmpty(message = "{apps_required}")
    @Size(min = 1)
    private List<App> apps;

    @Column(nullable = false, updatable = true)
    private Boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
