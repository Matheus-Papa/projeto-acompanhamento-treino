package br.com.treino.repository;

import br.com.treino.domain.MedidaCorporal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedidaCorporalRepository extends JpaRepository<MedidaCorporal, UUID> {

    List<MedidaCorporal> findByUsuarioIdOrderByDataDesc(UUID usuarioId);

    List<MedidaCorporal> findByUsuarioIdAndDataBetween(UUID usuarioId, LocalDate inicio, LocalDate fim);

    Optional<MedidaCorporal> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
