package br.com.treino;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.treino.dto.HistoricoItemResponse;
import br.com.treino.dto.PrResponse;
import br.com.treino.dto.RegisterRequest;
import br.com.treino.dto.SerieRequest;
import br.com.treino.dto.TreinoRequest;
import br.com.treino.dto.TreinoResponse;
import br.com.treino.dto.VolumeSemanalResponse;
import br.com.treino.exception.ConflictException;
import br.com.treino.exception.NotFoundException;
import br.com.treino.repository.FichaExercicioRepository;
import br.com.treino.repository.FichaRepository;
import br.com.treino.repository.SerieRepository;
import br.com.treino.repository.TreinoRepository;
import br.com.treino.repository.UsuarioRepository;
import br.com.treino.service.AuthService;
import br.com.treino.service.EstatisticaService;
import br.com.treino.service.TreinoService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

// Decisão: cenários via service (não via MockMvc) com commits reais — sem
// @Transactional no teste — para provar o rollback total com count() no banco
// após exceção. A camada HTTP é coberta em SegurancaIsolamentoTest via MockMvc.
class TreinoIntegrationTest extends AbstractIntegrationTest {

    // Primeiro exercício global do seed V2 (Supino reto com barra).
    private static final UUID EXERCICIO_SEED = UUID.fromString("11111111-1111-4111-8111-000000000001");

    @Autowired
    private TreinoService treinoService;

    @Autowired
    private EstatisticaService estatisticaService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TreinoRepository treinoRepository;

    @Autowired
    private SerieRepository serieRepository;

    @Autowired
    private FichaRepository fichaRepository;

    @Autowired
    private FichaExercicioRepository fichaExercicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limpar() {
        // Ordem respeita as FKs com RESTRICT (serie/ficha_exercicio antes dos donos).
        // Exercícios globais do seed são preservados; os do usuário caem em cascata no banco.
        serieRepository.deleteAll();
        treinoRepository.deleteAll();
        fichaExercicioRepository.deleteAll();
        fichaRepository.deleteAll();
        usuarioRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void desautenticar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraTreinoComSeriesERetornaPrAmbos() {
        registrarEAutenticar("feliz@teste.com");

        TreinoRequest request = new TreinoRequest(
                null,
                LocalDate.now(),
                60,
                "treino de peito",
                List.of(
                        new SerieRequest(EXERCICIO_SEED, 1, 10, new BigDecimal("80.00"), 8),
                        new SerieRequest(EXERCICIO_SEED, 2, 8, new BigDecimal("80.00"), 9)));

        TreinoResponse resposta = treinoService.registrar(request);

        assertNotNull(resposta.id());
        assertEquals(2, resposta.series().size());
        assertEquals(1, resposta.prs().size());
        assertEquals(EXERCICIO_SEED, resposta.prs().get(0).exercicioId());
        assertEquals(PrResponse.TipoPr.AMBOS, resposta.prs().get(0).tipo());
        assertEquals(1, treinoRepository.count());
        assertEquals(2, serieRepository.count());
    }

    @Test
    void serieInvalidaGeraExcecaoERollbackTotal() {
        registrarEAutenticar("rollback-serie@teste.com");

        TreinoRequest request = new TreinoRequest(
                null,
                LocalDate.now(),
                60,
                null,
                List.of(
                        new SerieRequest(EXERCICIO_SEED, 1, 10, new BigDecimal("80.00"), null),
                        new SerieRequest(EXERCICIO_SEED, 2, 0, new BigDecimal("80.00"), null)));

        assertThrows(ConflictException.class, () -> treinoService.registrar(request));
        assertEquals(0, treinoRepository.count());
        assertEquals(0, serieRepository.count());
    }

    @Test
    void exercicioInacessivelGera404ERollbackTotal() {
        registrarEAutenticar("rollback-exercicio@teste.com");

        TreinoRequest request = new TreinoRequest(
                null,
                LocalDate.now(),
                60,
                null,
                List.of(new SerieRequest(UUID.randomUUID(), 1, 10, new BigDecimal("80.00"), null)));

        assertThrows(NotFoundException.class, () -> treinoService.registrar(request));
        assertEquals(0, treinoRepository.count());
        assertEquals(0, serieRepository.count());
    }

    @Test
    void historicoPorExercicioAgrupaPorTreinoEmOrdemAsc() {
        registrarEAutenticar("historico@teste.com");
        LocalDate hoje = LocalDate.now();

        treinoService.registrar(new TreinoRequest(
                null, hoje.minusDays(7), 60, null,
                List.of(new SerieRequest(EXERCICIO_SEED, 1, 10, new BigDecimal("80.00"), null))));
        treinoService.registrar(new TreinoRequest(
                null, hoje.minusDays(1), 60, null,
                List.of(
                        new SerieRequest(EXERCICIO_SEED, 1, 8, new BigDecimal("80.00"), null),
                        new SerieRequest(EXERCICIO_SEED, 2, 10, new BigDecimal("90.00"), null))));

        List<HistoricoItemResponse> historico = treinoService.historicoPorExercicio(EXERCICIO_SEED);

        assertEquals(2, historico.size());
        assertTrue(historico.get(0).data().isBefore(historico.get(1).data()));
        // Treino 1: 10 x 80 = 800 de volume; epley 80 * (1 + 10/30) = 106.67.
        assertEquals(0, historico.get(0).volume().compareTo(new BigDecimal("800.00")));
        assertEquals(0, historico.get(0).epley().compareTo(new BigDecimal("106.67")));
        // Treino 2: 8 x 80 + 10 x 90 = 1540; melhor epley = 90 x 10 = 120.00.
        assertEquals(0, historico.get(1).volume().compareTo(new BigDecimal("1540.00")));
        assertEquals(10, historico.get(1).melhorSerie().repeticoes());
        assertEquals(0, historico.get(1).melhorSerie().cargaKg().compareTo(new BigDecimal("90.00")));
        assertEquals(0, historico.get(1).epley().compareTo(new BigDecimal("120.00")));
    }

    @Test
    void volumeSemanalSomaTreinosDaMesmaSemanaIso() {
        registrarEAutenticar("volume@teste.com");
        // Segunda e quarta da mesma semana ISO, nunca no futuro (recua 1 semana se preciso).
        LocalDate segunda = LocalDate.now().with(DayOfWeek.MONDAY);
        if (segunda.plusDays(2).isAfter(LocalDate.now())) {
            segunda = segunda.minusWeeks(1);
        }
        LocalDate quarta = segunda.plusDays(2);

        treinoService.registrar(new TreinoRequest(
                null, segunda, 60, null,
                List.of(new SerieRequest(EXERCICIO_SEED, 1, 10, new BigDecimal("100.00"), null))));
        treinoService.registrar(new TreinoRequest(
                null, quarta, 60, null,
                List.of(new SerieRequest(EXERCICIO_SEED, 1, 5, new BigDecimal("50.00"), null))));

        List<VolumeSemanalResponse> volume =
                estatisticaService.volumeSemanal(segunda.minusDays(1), quarta.plusDays(1));

        assertEquals(1, volume.size());
        assertEquals(segunda, volume.get(0).semanaSegunda());
        // 10 x 100 + 5 x 50 = 1250.
        assertEquals(0, volume.get(0).volume().compareTo(new BigDecimal("1250.00")));
    }

    private void registrarEAutenticar(String email) {
        authService.register(new RegisterRequest("Teste", email, "senha12345"));
        UUID usuarioId = usuarioRepository.findByEmail(email).orElseThrow().getId();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId, null, List.of()));
    }
}
