package br.com.treino.repository;

import br.com.treino.domain.Treino;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreinoRepository extends JpaRepository<Treino, UUID> {

    Page<Treino> findByUsuarioIdAndDataBetween(UUID usuarioId, LocalDate inicio, LocalDate fim, Pageable pageable);

    Optional<Treino> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
