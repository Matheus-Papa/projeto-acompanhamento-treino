package br.com.treino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

// Testes puros de CalculoTreino: sem Spring, sem banco.
class CalculoTreinoTest {

    @Test
    void epleyComUmaRepeticaoRetornaAPropriaCarga() {
        assertEquals(new BigDecimal("80.00"), CalculoTreino.epley(new BigDecimal("80"), 1));
    }

    @Test
    void epleyDezRepeticoesAplicaFatorUmMaisRepsSobreTrinta() {
        // 100 * (1 + 10/30) = 133.333... -> 133.33 (escala 2, HALF_UP).
        assertEquals(new BigDecimal("133.33"), CalculoTreino.epley(new BigDecimal("100"), 10));
    }

    @Test
    void epleyComCargaZeroRetornaZero() {
        assertEquals(new BigDecimal("0.00"), CalculoTreino.epley(new BigDecimal("0.00"), 10));
    }

    @Test
    void epleyComMuitasRepeticoesMantemEscalaEDoisDecimais() {
        // 100 * (1 + 100/30) = 433.333... -> 433.33 (borda: reps alto não quebra a escala).
        assertEquals(new BigDecimal("433.33"), CalculoTreino.epley(new BigDecimal("100"), 100));
    }

    @Test
    void epleyRejeitaRepeticoesNaoPositivasECargaNula() {
        assertThrows(IllegalArgumentException.class, () -> CalculoTreino.epley(new BigDecimal("100"), 0));
        assertThrows(IllegalArgumentException.class, () -> CalculoTreino.epley(new BigDecimal("100"), -3));
        assertThrows(IllegalArgumentException.class, () -> CalculoTreino.epley(null, 10));
    }

    @Test
    void volumeMultiplicaRepsPelaCargaComDuasCasas() {
        assertEquals(new BigDecimal("800.00"), CalculoTreino.volume(10, new BigDecimal("80.00")));
        assertEquals(new BigDecimal("0.00"), CalculoTreino.volume(8, new BigDecimal("0.00")));
    }

    @Test
    void volumeRejeitaRepeticoesNaoPositivasECargaNula() {
        assertThrows(IllegalArgumentException.class, () -> CalculoTreino.volume(0, new BigDecimal("80.00")));
        assertThrows(IllegalArgumentException.class, () -> CalculoTreino.volume(10, null));
    }
}
