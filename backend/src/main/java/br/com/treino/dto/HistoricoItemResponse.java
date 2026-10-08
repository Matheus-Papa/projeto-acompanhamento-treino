package br.com.treino.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HistoricoItemResponse(
        UUID treinoId,
        LocalDate data,
        MelhorSerie melhorSerie,
        BigDecimal volume,
        BigDecimal epley) {

    public record MelhorSerie(
            Integer repeticoes,
            BigDecimal cargaKg) {
    }
}
