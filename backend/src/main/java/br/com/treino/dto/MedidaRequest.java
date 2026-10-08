package br.com.treino.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MedidaRequest(
        @NotNull(message = "Data é obrigatória")
        @PastOrPresent(message = "Data não pode ser futura")
        LocalDate data,

        @NotNull(message = "Peso é obrigatório")
        @DecimalMin(value = "0.01", message = "Peso deve ser maior que zero")
        BigDecimal pesoKg,

        @DecimalMin(value = "0", message = "Percentual de gordura não pode ser negativo")
        @DecimalMax(value = "100", message = "Percentual de gordura deve ser no máximo 100")
        BigDecimal percentualGordura) {
}
