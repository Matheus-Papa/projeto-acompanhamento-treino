package br.com.treino.dto;

import java.util.UUID;

public record ExercicioResponse(
        UUID id,
        String nome,
        String grupoMuscular,
        String equipamento,
        boolean global) {
}
