package br.com.treino.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TreinoResponse(
        UUID id,
        LocalDate data,
        Integer duracaoMin,
        String observacao,
        UUID fichaId,
        List<SerieResponse> series,
        List<PrResponse> prs) {
}
