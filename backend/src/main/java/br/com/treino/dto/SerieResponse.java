package br.com.treino.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SerieResponse(
        UUID id,
        Integer numero,
        Integer repeticoes,
        BigDecimal cargaKg,
        Integer rpe,
        ExercicioResponse exercicio) {
}
