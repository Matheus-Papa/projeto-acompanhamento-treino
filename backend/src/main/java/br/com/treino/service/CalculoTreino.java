package br.com.treino.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Cálculos de treino puros (sem estado); reusado na Fase 5 (recorde) e nas estatísticas.
public final class CalculoTreino {

    private CalculoTreino() {
    }

    // Fórmula de Epley: estimativa de 1RM = carga * (1 + reps/30), escala 2 HALF_UP.
    // Com 1 repetição o recorde é a própria carga (sem extrapolação).
    public static BigDecimal epley(BigDecimal carga, int reps) {
        if (carga == null) {
            throw new IllegalArgumentException("Carga é obrigatória");
        }
        if (reps <= 0) {
            throw new IllegalArgumentException("Repetições devem ser maiores que zero");
        }
        if (reps == 1) {
            return carga.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fator = BigDecimal.valueOf(reps)
                .divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP)
                .add(BigDecimal.ONE);
        return carga.multiply(fator).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal volume(int reps, BigDecimal carga) {
        if (carga == null) {
            throw new IllegalArgumentException("Carga é obrigatória");
        }
        if (reps <= 0) {
            throw new IllegalArgumentException("Repetições devem ser maiores que zero");
        }
        return carga.multiply(BigDecimal.valueOf(reps)).setScale(2, RoundingMode.HALF_UP);
    }
}
