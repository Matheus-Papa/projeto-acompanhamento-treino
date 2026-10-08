package br.com.treino.dto;

import java.util.UUID;

public record PrResponse(
        UUID exercicioId,
        TipoPr tipo) {

    public enum TipoPr {
        CARGA,
        E1RM,
        AMBOS
    }
}
