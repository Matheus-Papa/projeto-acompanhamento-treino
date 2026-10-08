package br.com.treino.service;

import br.com.treino.domain.MedidaCorporal;
import br.com.treino.domain.Usuario;
import br.com.treino.dto.MedidaRequest;
import br.com.treino.dto.MedidaResponse;
import br.com.treino.exception.NotFoundException;
import br.com.treino.exception.UnauthorizedException;
import br.com.treino.repository.MedidaCorporalRepository;
import br.com.treino.repository.UsuarioRepository;
import br.com.treino.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedidaService {

    private final MedidaCorporalRepository medidaRepository;
    private final UsuarioRepository usuarioRepository;

    public MedidaService(MedidaCorporalRepository medidaRepository, UsuarioRepository usuarioRepository) {
        this.medidaRepository = medidaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public MedidaResponse registrar(MedidaRequest request) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Usuario dono = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
        MedidaCorporal medida = new MedidaCorporal(
                dono, request.data(), request.pesoKg(), request.percentualGordura());
        return paraResposta(medidaRepository.save(medida));
    }

    // Sem paginação: histórico de medidas é curto; lista completa em ordem desc por data.
    @Transactional(readOnly = true)
    public List<MedidaResponse> listar() {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        return medidaRepository.findByUsuarioIdOrderByDataDesc(usuarioId).stream()
                .map(this::paraResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedidaResponse buscarPorId(UUID id) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        MedidaCorporal medida = medidaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new NotFoundException("Medida não encontrada"));
        return paraResposta(medida);
    }

    private MedidaResponse paraResposta(MedidaCorporal medida) {
        return new MedidaResponse(
                medida.getId(), medida.getData(), medida.getPesoKg(), medida.getPercentualGordura());
    }
}
