package br.com.treino.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record FichaItemRequest(
        @NotNull(message = "Exercício é obrigatório")
        UUID exercicioId,

        @NotNull(message = "Ordem é obrigatória")
        @Positive(message = "Ordem deve ser maior que zero")
        Integer ordem,

        @NotNull(message = "Séries alvo são obrigatórias")
        @Positive(message = "Séries alvo devem ser maiores que zero")
        Integer seriesAlvo,

        @NotNull(message = "Repetições alvo são obrigatórias")
        @Positive(message = "Repetições alvo devem ser maiores que zero")
        Integer repsAlvo) {
}
