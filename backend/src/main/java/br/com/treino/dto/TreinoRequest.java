package br.com.treino.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Se fichaId for informado e series vier vazio ou omitido, as séries são geradas
// a partir da ficha: número sequencial global, repetindo seriesAlvo x repsAlvo
// de cada item (na ordem da ficha) com carga zero e sem RPE. Sem ficha, ao menos
// uma série é obrigatória (validado no service com rollback total).
public record TreinoRequest(
        UUID fichaId,

        @NotNull(message = "Data é obrigatória")
        @PastOrPresent(message = "Data não pode ser futura")
        LocalDate data,

        @Positive(message = "Duração deve ser maior que zero")
        Integer duracaoMin,

        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String observacao,

        List<@Valid SerieRequest> series) {
}
