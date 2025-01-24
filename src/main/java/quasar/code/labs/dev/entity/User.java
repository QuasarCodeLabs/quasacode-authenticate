package quasar.code.labs.dev.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.mindrot.jbcrypt.BCrypt;

@Entity
@Setter
@Getter
@ToString
@RequiredArgsConstructor
@Table(name = "users")
public class User extends PanacheEntity {

    @NotEmpty
    @NotBlank
    @Column(unique = true, nullable = false, insertable = true)
    private String username;

    @Column(nullable = false, unique = true, insertable = true)
    @NotBlank(message = "{email_empty}")
    @Email(message = "{email_invalid}")
    private String email;

    @NotNull
    @Column(unique = true, nullable = false, insertable = true)
    @Size(min = 6, max = 72, message = "{length_password}")
    @NotBlank(message = "{password_empty}")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$", message = "{pattern_password}")
    private String password;

    @NotNull()
    @Column(nullable = false, insertable = true)
    private String role;

    @Column(nullable = false, updatable = true)
    private Boolean active;

    @PrePersist
    @PreUpdate
    public void hashPassword() {
        if (this.password != null && !this.password.isBlank() ) {
            this.password = BCrypt.hashpw(this.password, BCrypt.gensalt());
        }
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
