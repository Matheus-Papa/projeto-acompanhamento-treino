package br.com.treino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.treino.domain.Exercicio;
import br.com.treino.domain.Serie;
import br.com.treino.domain.Treino;
import br.com.treino.domain.Usuario;
import br.com.treino.dto.PrResponse;
import br.com.treino.dto.SerieRequest;
import br.com.treino.dto.TreinoRequest;
import br.com.treino.dto.TreinoResponse;
import br.com.treino.exception.ConflictException;
import br.com.treino.exception.NotFoundException;
import br.com.treino.repository.ExercicioRepository;
import br.com.treino.repository.FichaExercicioRepository;
import br.com.treino.repository.FichaRepository;
import br.com.treino.repository.SerieRepository;
import br.com.treino.repository.TreinoRepository;
import br.com.treino.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

// Lógica de PR exercida via TreinoService.registrar com repositories mockados (sem banco).
// O service lê o usuarioId do SecurityContext (CurrentUser), por isso cada teste
// autentica um UUID antes de chamar o service e limpa o contexto depois.
@ExtendWith(MockitoExtension.class)
class DeteccaoPrTest {

    @Mock
    private TreinoRepository treinoRepository;

    @Mock
    private SerieRepository serieRepository;

    @Mock
    private ExercicioRepository exercicioRepository;

    @Mock
    private FichaRepository fichaRepository;

    @Mock
    private FichaExercicioRepository fichaExercicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TreinoService treinoService;

    private UUID usuarioId;

    @BeforeEach
    void autenticar() {
        usuarioId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId, null, List.of()));
        Usuario usuario = new Usuario("Teste", "teste@teste.com", "hash");
        usuario.setId(usuarioId);
        lenient().when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        // Sem JPA o save não gera id: devolve a própria entidade para o mapeamento em DTO.
        lenient().when(treinoRepository.save(any(Treino.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @AfterEach
    void desautenticar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void primeiroTreinoComCargaGeraPrAmbos() {
        UUID exercicioId = UUID.randomUUID();
        Exercicio exercicio = exercicioComId(exercicioId);
        when(exercicioRepository.findAcessivelPorId(exercicioId, usuarioId))
                .thenReturn(Optional.of(exercicio));
        when(serieRepository.historicoPorExercicios(eq(usuarioId), anyList())).thenReturn(List.of());

        TreinoResponse resposta = treinoService.registrar(pedido(exercicioId, 10, "80.00"));

        assertEquals(1, resposta.prs().size());
        assertEquals(exercicioId, resposta.prs().get(0).exercicioId());
        assertEquals(PrResponse.TipoPr.AMBOS, resposta.prs().get(0).tipo());
    }

    @Test
    void cargaZeroNuncaGeraPr() {
        UUID exercicioId = UUID.randomUUID();
        Exercicio exercicio = exercicioComId(exercicioId);
        when(exercicioRepository.findAcessivelPorId(exercicioId, usuarioId))
                .thenReturn(Optional.of(exercicio));
        when(serieRepository.historicoPorExercicios(eq(usuarioId), anyList())).thenReturn(List.of());

        TreinoResponse resposta = treinoService.registrar(pedido(exercicioId, 10, "0.00"));

        assertTrue(resposta.prs().isEmpty());
    }

    @Test
    void cargaMaiorComEpleyMenorGeraPrSomenteCarga() {
        // Histórico: 80kg x 10 (epley 106.67). Novo: 90kg x 1 (epley 90.00).
        // Carga supera o máximo anterior, mas o epley não -> só CARGA.
        UUID exercicioId = UUID.randomUUID();
        Exercicio exercicio = exercicioComId(exercicioId);
        when(exercicioRepository.findAcessivelPorId(exercicioId, usuarioId))
                .thenReturn(Optional.of(exercicio));
        when(serieRepository.historicoPorExercicios(eq(usuarioId), anyList()))
                .thenReturn(List.of(serieHistorico(exercicio, 10, "80.00")));

        TreinoResponse resposta = treinoService.registrar(pedido(exercicioId, 1, "90.00"));

        assertEquals(1, resposta.prs().size());
        assertEquals(exercicioId, resposta.prs().get(0).exercicioId());
        assertEquals(PrResponse.TipoPr.CARGA, resposta.prs().get(0).tipo());
    }

    @Test
    void mesmaCargaComMaisRepsGeraPrSomenteE1rm() {
        // Histórico: 100kg x 5 (epley 116.67). Novo: 100kg x 10 (epley 133.33).
        // Carga empata (não supera), epley supera -> só E1RM.
        UUID exercicioId = UUID.randomUUID();
        Exercicio exercicio = exercicioComId(exercicioId);
        when(exercicioRepository.findAcessivelPorId(exercicioId, usuarioId))
                .thenReturn(Optional.of(exercicio));
        when(serieRepository.historicoPorExercicios(eq(usuarioId), anyList()))
                .thenReturn(List.of(serieHistorico(exercicio, 5, "100.00")));

        TreinoResponse resposta = treinoService.registrar(pedido(exercicioId, 10, "100.00"));

        assertEquals(1, resposta.prs().size());
        assertEquals(exercicioId, resposta.prs().get(0).exercicioId());
        assertEquals(PrResponse.TipoPr.E1RM, resposta.prs().get(0).tipo());
    }

    @Test
    void serieComRepeticoesZeroLancaExcecaoESemSave() {
        UUID exercicioId = UUID.randomUUID();
        TreinoRequest request = new TreinoRequest(
                null,
                LocalDate.now(),
                60,
                null,
                List.of(new SerieRequest(exercicioId, 1, 0, new BigDecimal("80.00"), null)));

        assertThrows(ConflictException.class, () -> treinoService.registrar(request));
        // Rollback conceitual: validação falha antes de qualquer persistência.
        verify(treinoRepository, never()).save(any(Treino.class));
    }

    @Test
    void exercicioInacessivelLanca404ESemSave() {
        UUID exercicioId = UUID.randomUUID();
        when(exercicioRepository.findAcessivelPorId(exercicioId, usuarioId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> treinoService.registrar(pedido(exercicioId, 10, "80.00")));
        verify(treinoRepository, never()).save(any(Treino.class));
    }

    private TreinoRequest pedido(UUID exercicioId, int repeticoes, String cargaKg) {
        return new TreinoRequest(
                null,
                LocalDate.now(),
                60,
                null,
                List.of(new SerieRequest(exercicioId, 1, repeticoes, new BigDecimal(cargaKg), null)));
    }

    private Exercicio exercicioComId(UUID exercicioId) {
        Exercicio exercicio = new Exercicio("Supino reto com barra", "peito", "barra", null);
        exercicio.setId(exercicioId);
        return exercicio;
    }

    private Serie serieHistorico(Exercicio exercicio, int repeticoes, String cargaKg) {
        // detectarPrs só lê exercicio/id, repeticoes e cargaKg da série histórica.
        return new Serie(null, exercicio, 1, repeticoes, new BigDecimal(cargaKg), null);
    }
}
