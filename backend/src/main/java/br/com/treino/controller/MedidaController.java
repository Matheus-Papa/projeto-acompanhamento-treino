package br.com.treino.controller;

import br.com.treino.dto.MedidaRequest;
import br.com.treino.dto.MedidaResponse;
import br.com.treino.service.MedidaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/medidas")
public class MedidaController {

    private final MedidaService medidaService;

    public MedidaController(MedidaService medidaService) {
        this.medidaService = medidaService;
    }

    @PostMapping
    public ResponseEntity<MedidaResponse> registrar(@Valid @RequestBody MedidaRequest request) {
        MedidaResponse resposta = medidaService.registrar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id())
                .toUri();
        return ResponseEntity.created(localizacao).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<MedidaResponse>> listar() {
        return ResponseEntity.ok(medidaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedidaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(medidaService.buscarPorId(id));
    }
}
