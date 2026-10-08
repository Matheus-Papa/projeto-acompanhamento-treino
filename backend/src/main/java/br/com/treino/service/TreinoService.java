package br.com.treino.service;

import br.com.treino.domain.Exercicio;
import br.com.treino.domain.Ficha;
import br.com.treino.domain.FichaExercicio;
import br.com.treino.domain.Serie;
import br.com.treino.domain.Treino;
import br.com.treino.domain.Usuario;
import br.com.treino.dto.ExercicioResponse;
import br.com.treino.dto.HistoricoItemResponse;
import br.com.treino.dto.PrResponse;
import br.com.treino.dto.SerieRequest;
import br.com.treino.dto.SerieResponse;
import br.com.treino.dto.TreinoRequest;
import br.com.treino.dto.TreinoResponse;
import br.com.treino.dto.TreinoResumoResponse;
import br.com.treino.exception.ConflictException;
import br.com.treino.exception.NotFoundException;
import br.com.treino.exception.UnauthorizedException;
import br.com.treino.repository.ExercicioRepository;
import br.com.treino.repository.FichaExercicioRepository;
import br.com.treino.repository.FichaRepository;
import br.com.treino.repository.SerieRepository;
import br.com.treino.repository.TreinoRepository;
import br.com.treino.repository.UsuarioRepository;
import br.com.treino.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreinoService {

    // Sem filtro de período: intervalo amplo equivale a "sem filtro" com a query Between existente.
    private static final LocalDate DATA_MINIMA_LISTAGEM = LocalDate.of(1970, 1, 1);
    private static final LocalDate DATA_MAXIMA_LISTAGEM = LocalDate.of(9999, 12, 31);

    private final TreinoRepository treinoRepository;
    private final SerieRepository serieRepository;
    private final ExercicioRepository exercicioRepository;
    private final FichaRepository fichaRepository;
    private final FichaExercicioRepository fichaExercicioRepository;
    private final UsuarioRepository usuarioRepository;

    public TreinoService(
            TreinoRepository treinoRepository,
            SerieRepository serieRepository,
            ExercicioRepository exercicioRepository,
            FichaRepository fichaRepository,
            FichaExercicioRepository fichaExercicioRepository,
            UsuarioRepository usuarioRepository) {
        this.treinoRepository = treinoRepository;
        this.serieRepository = serieRepository;
        this.exercicioRepository = exercicioRepository;
        this.fichaRepository = fichaRepository;
        this.fichaExercicioRepository = fichaExercicioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Transação única: qualquer série inválida desfaz treino + séries (rollback total).
    @Transactional
    public TreinoResponse registrar(TreinoRequest request) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        if (request.data().isAfter(LocalDate.now())) {
            throw new ConflictException("Data do treino não pode ser futura");
        }
        Ficha ficha = null;
        if (request.fichaId() != null) {
            ficha = fichaRepository.findByIdAndUsuarioId(request.fichaId(), usuarioId)
                    .orElseThrow(() -> new NotFoundException("Ficha não encontrada"));
        }
        List<SerieRequest> pedidos = request.series() == null ? List.of() : request.series();
        if (pedidos.isEmpty()) {
            if (ficha == null) {
                throw new ConflictException("Treino deve ter ao menos uma série");
            }
            pedidos = gerarSeriesDaFicha(ficha);
        }
        if (pedidos.isEmpty()) {
            throw new ConflictException("Treino deve ter ao menos uma série");
        }
        List<Exercicio> exercicios = new ArrayList<>();
        for (SerieRequest pedido : pedidos) {
            validarSerie(pedido);
            Exercicio exercicio = exercicioRepository.findAcessivelPorId(pedido.exercicioId(), usuarioId)
                    .orElseThrow(() -> new NotFoundException("Exercício não encontrado: " + pedido.exercicioId()));
            exercicios.add(exercicio);
        }
        // PRs calculados ANTES de salvar, contra o histórico já persistido.
        List<PrResponse> prs = detectarPrs(usuarioId, pedidos);
        Usuario dono = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida"));
        Treino treino = new Treino(
                dono, ficha, request.data(), request.duracaoMin(), normalizarObservacao(request.observacao()));
        for (int i = 0; i < pedidos.size(); i++) {
            SerieRequest pedido = pedidos.get(i);
            treino.addSerie(new Serie(
                    treino, exercicios.get(i), pedido.numero(), pedido.repeticoes(), pedido.cargaKg(), pedido.rpe()));
        }
        Treino salvo = treinoRepository.save(treino);
        return paraRespostaCompleta(salvo, prs);
    }

    @Transactional(readOnly = true)
    public Page<TreinoResumoResponse> listar(LocalDate de, LocalDate ate, int page, int size) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        LocalDate inicio = de != null ? de : DATA_MINIMA_LISTAGEM;
        LocalDate fim = ate != null ? ate : DATA_MAXIMA_LISTAGEM;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "data"));
        return treinoRepository.findByUsuarioIdAndDataBetween(usuarioId, inicio, fim, pageable)
                .map(this::paraResumo);
    }

    @Transactional(readOnly = true)
    public TreinoResponse buscarPorId(UUID id) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        Treino treino = treinoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new NotFoundException("Treino não encontrado"));
        // prs vazio no GET: PRs só existem na resposta do POST (recalcular incluiria o próprio treino).
        return paraRespostaCompleta(treino, List.of());
    }

    // Histórico agrupado por treino para GET /exercicios/{id}/historico (ordem de data asc).
    @Transactional(readOnly = true)
    public List<HistoricoItemResponse> historicoPorExercicio(UUID exercicioId) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        exercicioRepository.findAcessivelPorId(exercicioId, usuarioId)
                .orElseThrow(() -> new NotFoundException("Exercício não encontrado"));
        List<Serie> series = serieRepository.historicoPorExercicio(usuarioId, exercicioId);
        Map<UUID, List<Serie>> porTreino = new LinkedHashMap<>();
        for (Serie serie : series) {
            porTreino.computeIfAbsent(serie.getTreino().getId(), chave -> new ArrayList<>()).add(serie);
        }
        List<HistoricoItemResponse> resposta = new ArrayList<>();
        for (List<Serie> doTreino : porTreino.values()) {
            UUID treinoId = doTreino.get(0).getTreino().getId();
            LocalDate data = doTreino.get(0).getTreino().getData();
            Serie melhor = doTreino.stream()
                    .max((atual, outra) -> {
                        int comparacao = CalculoTreino.epley(atual.getCargaKg(), atual.getRepeticoes())
                                .compareTo(CalculoTreino.epley(outra.getCargaKg(), outra.getRepeticoes()));
                        if (comparacao != 0) {
                            return comparacao;
                        }
                        return atual.getCargaKg().compareTo(outra.getCargaKg());
                    })
                    .orElseThrow(() -> new ConflictException("Treino sem séries"));
            BigDecimal volume = doTreino.stream()
                    .map(serie -> CalculoTreino.volume(serie.getRepeticoes(), serie.getCargaKg()))
                    .reduce(new BigDecimal("0.00"), BigDecimal::add);
            BigDecimal epley = CalculoTreino.epley(melhor.getCargaKg(), melhor.getRepeticoes());
            resposta.add(new HistoricoItemResponse(
                    treinoId,
                    data,
                    new HistoricoItemResponse.MelhorSerie(melhor.getRepeticoes(), melhor.getCargaKg()),
                    volume,
                    epley));
        }
        return resposta;
    }

    // Geração a partir da ficha: número sequencial global, seriesAlvo x repsAlvo com carga zero.
    // N+1 didático e aceito: ficha tem poucos itens; Exercicio LAZY lido dentro da transação.
    private List<SerieRequest> gerarSeriesDaFicha(Ficha ficha) {
        List<FichaExercicio> itens = fichaExercicioRepository.findByFichaIdOrderByOrdemAsc(ficha.getId());
        List<SerieRequest> pedidos = new ArrayList<>();
        int numero = 1;
        for (FichaExercicio item : itens) {
            for (int i = 0; i < item.getSeriesAlvo(); i++) {
                pedidos.add(new SerieRequest(
                        item.getExercicio().getId(), numero++, item.getRepsAlvo(), new BigDecimal("0.00"), null));
            }
        }
        return pedidos;
    }

    // Defesa em profundidade: o controller já valida via Bean Validation; o service garante rollback
    // mesmo em chamada direta.
    private void validarSerie(SerieRequest pedido) {
        if (pedido.exercicioId() == null) {
            throw new ConflictException("Exercício é obrigatório");
        }
        if (pedido.numero() == null || pedido.numero() <= 0) {
            throw new ConflictException("Número deve ser maior que zero");
        }
        if (pedido.repeticoes() == null || pedido.repeticoes() <= 0) {
            throw new ConflictException("Repetições devem ser maiores que zero");
        }
        if (pedido.cargaKg() == null || pedido.cargaKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Carga não pode ser negativa");
        }
        if (pedido.rpe() != null && (pedido.rpe() < 1 || pedido.rpe() > 10)) {
            throw new ConflictException("RPE deve ser entre 1 e 10");
        }
    }

    // Primeira vez com carga > 0 conta como PR AMBOS; carga zero nunca gera PR.
    // 1 query com IN por todos os exercícios; máximos agrupados em memória por exercício.
    private List<PrResponse> detectarPrs(UUID usuarioId, List<SerieRequest> pedidos) {
        Map<UUID, BigDecimal> novaMaxCarga = new HashMap<>();
        Map<UUID, BigDecimal> novoMaxEpley = new HashMap<>();
        for (SerieRequest pedido : pedidos) {
            BigDecimal epley = CalculoTreino.epley(pedido.cargaKg(), pedido.repeticoes());
            novaMaxCarga.merge(pedido.exercicioId(), pedido.cargaKg(), BigDecimal::max);
            novoMaxEpley.merge(pedido.exercicioId(), epley, BigDecimal::max);
        }
        List<UUID> exercicioIds = new ArrayList<>(novaMaxCarga.keySet());
        List<Serie> historico = exercicioIds.isEmpty()
                ? List.of()
                : serieRepository.historicoPorExercicios(usuarioId, exercicioIds);
        Map<UUID, BigDecimal> maxCargaAnterior = new HashMap<>();
        Map<UUID, BigDecimal> maxEpleyAnterior = new HashMap<>();
        for (Serie serie : historico) {
            UUID exercicioId = serie.getExercicio().getId();
            BigDecimal epley = CalculoTreino.epley(serie.getCargaKg(), serie.getRepeticoes());
            maxCargaAnterior.merge(exercicioId, serie.getCargaKg(), BigDecimal::max);
            maxEpleyAnterior.merge(exercicioId, epley, BigDecimal::max);
        }
        List<PrResponse> prs = new ArrayList<>();
        for (UUID exercicioId : novaMaxCarga.keySet()) {
            BigDecimal novaCarga = novaMaxCarga.get(exercicioId);
            BigDecimal novoEpley = novoMaxEpley.get(exercicioId);
            boolean prCarga;
            boolean prEpley;
            if (!maxCargaAnterior.containsKey(exercicioId)) {
                prCarga = novaCarga.compareTo(BigDecimal.ZERO) > 0;
                prEpley = novaCarga.compareTo(BigDecimal.ZERO) > 0;
            } else {
                prCarga = novaCarga.compareTo(maxCargaAnterior.get(exercicioId)) > 0;
                prEpley = novoEpley.compareTo(maxEpleyAnterior.get(exercicioId)) > 0;
            }
            if (prCarga && prEpley) {
                prs.add(new PrResponse(exercicioId, PrResponse.TipoPr.AMBOS));
            } else if (prCarga) {
                prs.add(new PrResponse(exercicioId, PrResponse.TipoPr.CARGA));
            } else if (prEpley) {
                prs.add(new PrResponse(exercicioId, PrResponse.TipoPr.E1RM));
            }
        }
        return prs;
    }

    private TreinoResumoResponse paraResumo(Treino treino) {
        // N+1 didático e aceito: página pequena (padrão 10); coleção LAZY lida dentro da transação.
        List<Serie> series = treino.getSeries();
        BigDecimal volumeTotal = series.stream()
                .map(serie -> CalculoTreino.volume(serie.getRepeticoes(), serie.getCargaKg()))
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new TreinoResumoResponse(
                treino.getId(),
                treino.getData(),
                treino.getDuracaoMin(),
                treino.getFicha() != null ? treino.getFicha().getId() : null,
                series.size(),
                volumeTotal);
    }

    // N+1 didático e aceito: treino tem poucas séries; coleção e Exercicio LAZY lidos dentro da transação.
    private TreinoResponse paraRespostaCompleta(Treino treino, List<PrResponse> prs) {
        List<SerieResponse> series = treino.getSeries().stream()
                .sorted(Comparator.comparing(Serie::getNumero))
                .map(this::paraSerieResposta)
                .toList();
        return new TreinoResponse(
                treino.getId(),
                treino.getData(),
                treino.getDuracaoMin(),
                treino.getObservacao(),
                treino.getFicha() != null ? treino.getFicha().getId() : null,
                series,
                prs);
    }

    private SerieResponse paraSerieResposta(Serie serie) {
        // N+1 didático e aceito: ver paraRespostaCompleta; um acesso LAZY por série do treino.
        Exercicio exercicio = serie.getExercicio();
        ExercicioResponse exercicioResposta = new ExercicioResponse(
                exercicio.getId(),
                exercicio.getNome(),
                exercicio.getGrupoMuscular(),
                exercicio.getEquipamento(),
                exercicio.getUsuario() == null);
        return new SerieResponse(
                serie.getId(),
                serie.getNumero(),
                serie.getRepeticoes(),
                serie.getCargaKg(),
                serie.getRpe(),
                exercicioResposta);
    }

    private String normalizarObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }
        return observacao.trim();
    }
}
