package br.com.treino.service;

import br.com.treino.domain.Exercicio;
import br.com.treino.domain.Ficha;
import br.com.treino.domain.FichaExercicio;
import br.com.treino.domain.Usuario;
import br.com.treino.dto.ExercicioResponse;
import br.com.treino.dto.FichaItemRequest;
import br.com.treino.dto.FichaItemResponse;
import br.com.treino.dto.FichaRequest;
import br.com.treino.dto.FichaResponse;
import br.com.treino.exception.ConflictException;
import br.com.treino.exception.NotFoundException;
import br.com.treino.exception.UnauthorizedException;
import br.com.treino.repository.ExercicioRepository;
import br.com.treino.repository.FichaExercicioRepository;
import br.com.treino.repository.FichaRepository;
import br.com.treino.repository.UsuarioRepository;
import br.com.treino.security.CurrentUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FichaService {

    private final FichaRepository fichaRepository;
    private final FichaExercicioRepository fichaExercicioRepository;
    private final ExercicioRepository exercicioRepository;
    private final UsuarioRepository usuarioRepository;

    public FichaService(
            FichaRepository fichaRepository,
            FichaExercicioRepository fichaExercicioRepository,
            ExercicioRepository exercicioRepository,
            UsuarioRepository usuarioRepository) {
        this.fichaRepository = fichaRepository;
        this.fichaExercicioRepository = fichaExercicioRepository;
        this.exercicioRepository = exercicioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<FichaResponse> listar() {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        List<Ficha> fichas = fichaRepository.findByUsuarioIdOrderByNome(usuarioId);
        // N+1 didático e aceito: volume pequeno de fichas por usuário; uma query extra por ficha.
        List<FichaResponse> respostas = new ArrayList<>();
        for (Ficha ficha : fichas) {
            List<FichaExercicio> itens = fichaExercicioRepository.findByFichaIdOrderByOrdemAsc(ficha.getId());
            respostas.add(paraResposta(ficha, itens));
        }
        return respostas;
    }

    @Transactional(readOnly = true)
    public FichaResponse buscarPorId(UUID id) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Ficha ficha = fichaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new NotFoundException("Ficha não encontrada"));
        List<FichaExercicio> itens = fichaExercicioRepository.findByFichaIdOrderByOrdemAsc(ficha.getId());
        return paraResposta(ficha, itens);
    }

    // Transação: qualquer item inválido desfaz a ficha inteira (rollback total).
    @Transactional
    public FichaResponse criar(FichaRequest request) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Usuario dono = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
        Ficha ficha = new Ficha(request.nome().trim(), dono);
        Ficha salva = fichaRepository.save(ficha);
        List<FichaExercicio> itens = validarEMontarItens(salva, request.itens(), usuarioId);
        for (FichaExercicio item : itens) {
            salva.addItem(item);
        }
        fichaExercicioRepository.saveAll(itens);
        return paraResposta(salva, ordenar(itens));
    }

    // Transação: substitui os itens via orphanRemoval (clear + recriação); falha em qualquer item desfaz tudo.
    @Transactional
    public FichaResponse atualizar(UUID id, FichaRequest request) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Ficha ficha = fichaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new NotFoundException("Ficha não encontrada"));
        ficha.setNome(request.nome().trim());
        // Sem bulk delete redundante: clear com orphanRemoval apaga as linhas órfãs.
        ficha.getItens().clear();
        List<FichaExercicio> novos = validarEMontarItens(ficha, request.itens(), usuarioId);
        for (FichaExercicio item : novos) {
            ficha.addItem(item);
        }
        fichaExercicioRepository.saveAll(novos);
        return paraResposta(ficha, ordenar(novos));
    }

    @Transactional
    public void excluir(UUID id) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Ficha ficha = fichaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new NotFoundException("Ficha não encontrada"));
        fichaRepository.delete(ficha);
    }

    // Passada única: valida e monta guardando Exercicio num Map (evita buscar 2x).
    private List<FichaExercicio> validarEMontarItens(
            Ficha ficha, List<FichaItemRequest> pedidos, UUID usuarioId) {
        Set<Integer> ordens = new HashSet<>();
        Map<UUID, Exercicio> exercicios = new HashMap<>();
        List<FichaExercicio> itens = new ArrayList<>();
        for (FichaItemRequest pedido : pedidos) {
            if (pedido.ordem() == null || pedido.ordem() < 1) {
                throw new ConflictException("Ordem deve ser maior que zero");
            }
            if (!ordens.add(pedido.ordem())) {
                throw new ConflictException("Ordem duplicada na ficha: " + pedido.ordem());
            }
            if (pedido.seriesAlvo() == null || pedido.seriesAlvo() < 1) {
                throw new ConflictException("Séries alvo devem ser maiores que zero");
            }
            if (pedido.repsAlvo() == null || pedido.repsAlvo() < 1) {
                throw new ConflictException("Repetições alvo devem ser maiores que zero");
            }
            Exercicio exercicio = exercicios.computeIfAbsent(
                    pedido.exercicioId(),
                    exercicioId -> exercicioRepository.findAcessivelPorId(exercicioId, usuarioId)
                            .orElseThrow(() ->
                                    new NotFoundException("Exercício não encontrado: " + exercicioId)));
            itens.add(new FichaExercicio(
                    ficha, exercicio, pedido.ordem(), pedido.seriesAlvo(), pedido.repsAlvo()));
        }
        return ordenar(itens);
    }

    private List<FichaExercicio> ordenar(List<FichaExercicio> itens) {
        return itens.stream()
                .sorted(Comparator.comparing(FichaExercicio::getOrdem))
                .toList();
    }

    private FichaResponse paraResposta(Ficha ficha, List<FichaExercicio> itens) {
        List<FichaItemResponse> respostas = ordenar(itens).stream()
                .map(this::paraItemResposta)
                .toList();
        return new FichaResponse(ficha.getId(), ficha.getNome(), respostas);
    }

    private FichaItemResponse paraItemResposta(FichaExercicio item) {
        // N+1 didático e aceito: ficha tem poucos itens; Exercicio LAZY lido dentro da transação.
        Exercicio exercicio = item.getExercicio();
        ExercicioResponse exercicioResposta = new ExercicioResponse(
                exercicio.getId(),
                exercicio.getNome(),
                exercicio.getGrupoMuscular(),
                exercicio.getEquipamento(),
                exercicio.getUsuario() == null);
        return new FichaItemResponse(
                item.getOrdem(), item.getSeriesAlvo(), item.getRepsAlvo(), exercicioResposta);
    }
}
