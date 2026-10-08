package br.com.treino.service;

import br.com.treino.domain.Serie;
import br.com.treino.domain.Treino;
import br.com.treino.dto.RecordeResponse;
import br.com.treino.dto.VolumeSemanalResponse;
import br.com.treino.exception.NotFoundException;
import br.com.treino.repository.ExercicioRepository;
import br.com.treino.repository.SerieRepository;
import br.com.treino.repository.TreinoRepository;
import br.com.treino.security.CurrentUser;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstatisticaService {

    private final ExercicioRepository exercicioRepository;
    private final SerieRepository serieRepository;
    private final TreinoRepository treinoRepository;

    public EstatisticaService(
            ExercicioRepository exercicioRepository,
            SerieRepository serieRepository,
            TreinoRepository treinoRepository) {
        this.exercicioRepository = exercicioRepository;
        this.serieRepository = serieRepository;
        this.treinoRepository = treinoRepository;
    }

    @Transactional(readOnly = true)
    public RecordeResponse recordePorExercicio(UUID exercicioId) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        // Isolamento: exercício de outro usuário responde 404, sem vazar existência.
        exercicioRepository.findAcessivelPorId(exercicioId, usuarioId)
                .orElseThrow(() -> new NotFoundException("Exercício não encontrado"));
        // Já vem ordenado por data asc; o empate no maxEpley fica com o mais recente (>=).
        List<Serie> historico = serieRepository.historicoPorExercicio(usuarioId, exercicioId);
        if (historico.isEmpty()) {
            throw new NotFoundException("Sem treinos para este exercício");
        }
        BigDecimal maxCarga = null;
        BigDecimal maxEpley = null;
        Serie serieDoRecorde = null;
        for (Serie serie : historico) {
            if (maxCarga == null || serie.getCargaKg().compareTo(maxCarga) > 0) {
                maxCarga = serie.getCargaKg();
            }
            // Epley com 1 repetição retorna a própria carga (sem extrapolação).
            BigDecimal epley = CalculoTreino.epley(serie.getCargaKg(), serie.getRepeticoes());
            if (maxEpley == null || epley.compareTo(maxEpley) >= 0) {
                maxEpley = epley;
                serieDoRecorde = serie;
            }
        }
        Treino treinoDoRecorde = serieDoRecorde.getTreino();
        return new RecordeResponse(
                exercicioId, maxCarga, maxEpley, treinoDoRecorde.getId(), treinoDoRecorde.getData());
    }

    @Transactional(readOnly = true)
    public List<VolumeSemanalResponse> volumeSemanal(LocalDate de, LocalDate ate) {
        UUID usuarioId = CurrentUser.currentUsuarioId();
        LocalDate fim = ate != null ? ate : LocalDate.now();
        LocalDate inicio = de != null ? de : fim.minusDays(56);
        List<Treino> treinos = treinoRepository
                .findByUsuarioIdAndDataBetween(usuarioId, inicio, fim, Pageable.unpaged())
                .getContent();
        // Semana ISO: segunda-feira da data do treino; TreeMap já ordena asc.
        // Só entram semanas com treino (chave criada sob demanda no merge).
        Map<LocalDate, BigDecimal> porSemana = new TreeMap<>();
        for (Treino treino : treinos) {
            // N+1 didático e aceito: janela curta (padrão 8 semanas) com poucos treinos.
            List<Serie> series = serieRepository.findByTreinoIdOrderByNumeroAsc(treino.getId());
            BigDecimal volumeTreino = series.stream()
                    .map(serie -> CalculoTreino.volume(serie.getRepeticoes(), serie.getCargaKg()))
                    .reduce(new BigDecimal("0.00"), BigDecimal::add);
            LocalDate segunda = treino.getData().with(DayOfWeek.MONDAY);
            porSemana.merge(segunda, volumeTreino, BigDecimal::add);
        }
        return porSemana.entrySet().stream()
                .map(entrada -> new VolumeSemanalResponse(entrada.getKey(), entrada.getValue()))
                .toList();
    }
}
