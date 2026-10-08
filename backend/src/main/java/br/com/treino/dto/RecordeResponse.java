package br.com.treino.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordeResponse(
        UUID exercicioId,
        BigDecimal maxCarga,
        BigDecimal maxEpley,
        UUID emTreinoId,
        LocalDate emData) {
}
