package br.com.treino.service;

import br.com.treino.domain.Exercicio;
import br.com.treino.domain.Usuario;
import br.com.treino.dto.ExercicioRequest;
import br.com.treino.dto.ExercicioResponse;
import br.com.treino.exception.NotFoundException;
import br.com.treino.exception.UnauthorizedException;
import br.com.treino.repository.ExercicioRepository;
import br.com.treino.repository.UsuarioRepository;
import br.com.treino.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExercicioService {

    private final ExercicioRepository exercicioRepository;
    private final UsuarioRepository usuarioRepository;

    public ExercicioService(ExercicioRepository exercicioRepository, UsuarioRepository usuarioRepository) {
        this.exercicioRepository = exercicioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ExercicioResponse> listar() {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        return exercicioRepository.findAcessiveis(usuarioId).stream()
                .map(this::paraResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExercicioResponse buscarPorId(UUID id) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Exercicio exercicio = exercicioRepository.findAcessivelPorId(id, usuarioId)
                .orElseThrow(() -> new NotFoundException("Exercício não encontrado"));
        return paraResposta(exercicio);
    }

    @Transactional
    public ExercicioResponse criar(ExercicioRequest request) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Usuario dono = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
        Exercicio exercicio = new Exercicio(
                request.nome().trim(),
                request.grupoMuscular().trim(),
                normalizarEquipamento(request.equipamento()),
                dono);
        Exercicio salvo = exercicioRepository.save(exercicio);
        return paraResposta(salvo);
    }

    @Transactional
    public ExercicioResponse atualizar(UUID id, ExercicioRequest request) {
        Exercicio exercicio = buscarProprioOu404(id);
        exercicio.setNome(request.nome().trim());
        exercicio.setGrupoMuscular(request.grupoMuscular().trim());
        exercicio.setEquipamento(normalizarEquipamento(request.equipamento()));
        return paraResposta(exercicioRepository.save(exercicio));
    }

    @Transactional
    public void excluir(UUID id) {
        Exercicio exercicio = buscarProprioOu404(id);
        exercicioRepository.delete(exercicio);
    }

    // Isolamento: global ou de outro usuário responde 404 (nunca 403).
    private Exercicio buscarProprioOu404(UUID id) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        return exercicioRepository.findProprioPorId(id, usuarioId)
                .orElseThrow(() -> new NotFoundException("Exercício não encontrado"));
    }

    private ExercicioResponse paraResposta(Exercicio exercicio) {
        return new ExercicioResponse(
                exercicio.getId(),
                exercicio.getNome(),
                exercicio.getGrupoMuscular(),
                exercicio.getEquipamento(),
                exercicio.getUsuario() == null);
    }

    private String normalizarEquipamento(String equipamento) {
        if (equipamento == null || equipamento.isBlank()) {
            return null;
        }
        return equipamento.trim();
    }
}
