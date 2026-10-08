package br.com.treino.repository;

import br.com.treino.domain.Serie;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerieRepository extends JpaRepository<Serie, UUID> {

    List<Serie> findByTreinoIdOrderByNumeroAsc(UUID treinoId);

    // Histórico de um exercício: JOIN FETCH no treino evita N+1 ao ler a data.
    @Query("SELECT s FROM Serie s JOIN FETCH s.treino t WHERE t.usuario.id = :usuarioId AND s.exercicio.id = :exercicioId ORDER BY t.data ASC, s.numero ASC")
    List<Serie> historicoPorExercicio(@Param("usuarioId") UUID usuarioId, @Param("exercicioId") UUID exercicioId);

    // PRs em 1 query: busca todos os exercícios do treino de uma vez; agrupa em memória.
    @Query("SELECT s FROM Serie s JOIN FETCH s.treino t JOIN FETCH s.exercicio e WHERE t.usuario.id = :usuarioId AND s.exercicio.id IN :exercicioIds ORDER BY t.data ASC, s.numero ASC")
    List<Serie> historicoPorExercicios(
            @Param("usuarioId") UUID usuarioId, @Param("exercicioIds") List<UUID> exercicioIds);
}
