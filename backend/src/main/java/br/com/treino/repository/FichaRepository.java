package br.com.treino.repository;

import br.com.treino.domain.Ficha;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaRepository extends JpaRepository<Ficha, UUID> {

    List<Ficha> findByUsuarioIdOrderByNome(UUID usuarioId);

    Optional<Ficha> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
