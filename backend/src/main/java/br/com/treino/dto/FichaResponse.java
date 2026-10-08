package br.com.treino.dto;

import java.util.List;
import java.util.UUID;

public record FichaResponse(
        UUID id,
        String nome,
        List<FichaItemResponse> itens) {
}
