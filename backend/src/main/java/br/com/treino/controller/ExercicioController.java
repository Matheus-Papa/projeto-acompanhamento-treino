package br.com.treino.controller;

import br.com.treino.dto.ExercicioRequest;
import br.com.treino.dto.ExercicioResponse;
import br.com.treino.dto.HistoricoItemResponse;
import br.com.treino.dto.RecordeResponse;
import br.com.treino.service.EstatisticaService;
import br.com.treino.service.ExercicioService;
import br.com.treino.service.TreinoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/exercicios")
public class ExercicioController {

    private final ExercicioService exercicioService;
    private final TreinoService treinoService;
    private final EstatisticaService estatisticaService;

    public ExercicioController(
            ExercicioService exercicioService,
            TreinoService treinoService,
            EstatisticaService estatisticaService) {
        this.exercicioService = exercicioService;
        this.treinoService = treinoService;
        this.estatisticaService = estatisticaService;
    }

    @GetMapping
    public ResponseEntity<List<ExercicioResponse>> listar() {
        return ResponseEntity.ok(exercicioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExercicioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(exercicioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ExercicioResponse> criar(@Valid @RequestBody ExercicioRequest request) {
        ExercicioResponse resposta = exercicioService.criar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(resposta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExercicioResponse> atualizar(
            @PathVariable UUID id, @Valid @RequestBody ExercicioRequest request) {
        return ResponseEntity.ok(exercicioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        exercicioService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoItemResponse>> historico(@PathVariable UUID id) {
        return ResponseEntity.ok(treinoService.historicoPorExercicio(id));
    }

    @GetMapping("/{id}/recorde")
    public ResponseEntity<RecordeResponse> recorde(@PathVariable UUID id) {
        return ResponseEntity.ok(estatisticaService.recordePorExercicio(id));
    }
}
