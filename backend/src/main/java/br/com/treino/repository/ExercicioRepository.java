package br.com.treino.repository;

import br.com.treino.domain.Exercicio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExercicioRepository extends JpaRepository<Exercicio, UUID> {

    // Isolamento: enxerga os globais (usuario NULL) mais os próprios.
    // LEFT JOIN explícito: navegação e.usuario.id geraria INNER JOIN e sumiria com os globais.
    @Query("SELECT e FROM Exercicio e LEFT JOIN e.usuario u WHERE u.id IS NULL OR u.id = :usuarioId ORDER BY e.nome ASC")
    List<Exercicio> findAcessiveis(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT e FROM Exercicio e LEFT JOIN e.usuario u WHERE e.id = :id AND (u.id IS NULL OR u.id = :usuarioId)")
    Optional<Exercicio> findAcessivelPorId(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

    // Edição/exclusão: próprio apenas, sem global; 404 direto sem lazy.
    @Query("SELECT e FROM Exercicio e WHERE e.id = :id AND e.usuario.id = :usuarioId")
    Optional<Exercicio> findProprioPorId(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);
}
