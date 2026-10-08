package br.com.treino.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record SerieRequest(
        @NotNull(message = "Exercício é obrigatório")
        UUID exercicioId,

        @NotNull(message = "Número é obrigatório")
        @Positive(message = "Número deve ser maior que zero")
        Integer numero,

        @NotNull(message = "Repetições são obrigatórias")
        @Positive(message = "Repetições devem ser maiores que zero")
        Integer repeticoes,

        @NotNull(message = "Carga é obrigatória")
        @DecimalMin(value = "0.00", message = "Carga não pode ser negativa")
        BigDecimal cargaKg,

        @Min(value = 1, message = "RPE deve ser entre 1 e 10")
        @Max(value = 10, message = "RPE deve ser entre 1 e 10")
        Integer rpe) {
}
