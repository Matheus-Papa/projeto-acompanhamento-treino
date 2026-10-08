package br.com.treino.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VolumeSemanalResponse(
        LocalDate semanaSegunda,
        BigDecimal volume) {
}
