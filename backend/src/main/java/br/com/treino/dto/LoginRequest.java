package br.com.treino.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        String senha) {
}
