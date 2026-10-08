package br.com.treino.controller;

import br.com.treino.dto.FichaRequest;
import br.com.treino.dto.FichaResponse;
import br.com.treino.service.FichaService;
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
@RequestMapping("/fichas")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @GetMapping
    public ResponseEntity<List<FichaResponse>> listar() {
        return ResponseEntity.ok(fichaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FichaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(fichaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<FichaResponse> criar(@Valid @RequestBody FichaRequest request) {
        FichaResponse resposta = fichaService.criar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(resposta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FichaResponse> atualizar(
            @PathVariable UUID id, @Valid @RequestBody FichaRequest request) {
        return ResponseEntity.ok(fichaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        fichaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
