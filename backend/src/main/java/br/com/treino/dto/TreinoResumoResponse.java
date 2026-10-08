package br.com.treino.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Versão leve para a listagem paginada (sem as séries detalhadas).
public record TreinoResumoResponse(
        UUID id,
        LocalDate data,
        Integer duracaoMin,
        UUID fichaId,
        int totalSeries,
        BigDecimal volumeTotal) {
}
