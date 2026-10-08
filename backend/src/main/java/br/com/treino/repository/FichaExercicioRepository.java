package br.com.treino.repository;

import br.com.treino.domain.FichaExercicio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaExercicioRepository extends JpaRepository<FichaExercicio, UUID> {

    List<FichaExercicio> findByFichaIdOrderByOrdemAsc(UUID fichaId);

    void deleteByFichaId(UUID fichaId);
}
