package br.com.treino.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExercicioRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "Grupo muscular é obrigatório")
        @Size(max = 60, message = "Grupo muscular deve ter no máximo 60 caracteres")
        String grupoMuscular,

        @Size(max = 60, message = "Equipamento deve ter no máximo 60 caracteres")
        String equipamento) {
}
