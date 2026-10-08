package br.com.treino.controller;

import br.com.treino.dto.TreinoRequest;
import br.com.treino.dto.TreinoResponse;
import br.com.treino.dto.TreinoResumoResponse;
import br.com.treino.service.TreinoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/treinos")
public class TreinoController {

    private final TreinoService treinoService;

    public TreinoController(TreinoService treinoService) {
        this.treinoService = treinoService;
    }

    @PostMapping
    public ResponseEntity<TreinoResponse> registrar(@Valid @RequestBody TreinoRequest request) {
        TreinoResponse resposta = treinoService.registrar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<TreinoResumoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "Página deve ser zero ou maior") int page,
            @RequestParam(defaultValue = "10") @Positive(message = "Tamanho deve ser maior que zero") @Max(value = 100, message = "Tamanho deve ser no máximo 100") int size) {
        return ResponseEntity.ok(treinoService.listar(de, ate, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreinoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(treinoService.buscarPorId(id));
    }
}
