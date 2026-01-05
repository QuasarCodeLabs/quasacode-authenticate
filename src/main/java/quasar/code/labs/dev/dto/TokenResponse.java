package quasar.code.labs.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@RegisterForReflection
public class TokenResponse {
    @JsonProperty("token")  // 🔹 Indica a Jackson que este campo debe serializarse
    private String token;

    public TokenResponse(String token) {
        this.token = token;
    }

}