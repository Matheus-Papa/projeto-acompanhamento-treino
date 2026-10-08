package br.com.treino.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MedidaResponse(
        UUID id,
        LocalDate data,
        BigDecimal pesoKg,
        BigDecimal percentualGordura) {
}
