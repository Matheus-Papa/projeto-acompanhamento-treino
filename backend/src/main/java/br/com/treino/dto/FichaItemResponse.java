package br.com.treino.dto;

public record FichaItemResponse(
        Integer ordem,
        Integer seriesAlvo,
        Integer repsAlvo,
        ExercicioResponse exercicio) {
}
