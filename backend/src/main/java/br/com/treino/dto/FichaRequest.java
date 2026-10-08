package br.com.treino.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FichaRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        @NotNull(message = "Itens são obrigatórios")
        @Size(min = 1, message = "Ficha deve ter ao menos um exercício")
        List<@Valid FichaItemRequest> itens) {
}
