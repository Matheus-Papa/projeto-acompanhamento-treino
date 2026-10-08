package br.com.treino.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ficha_exercicio")
public class FichaExercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ficha_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fichaexercicio_ficha"))
    private Ficha ficha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercicio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fichaexercicio_exercicio"))
    private Exercicio exercicio;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Column(name = "series_alvo", nullable = false)
    private Integer seriesAlvo;

    @Column(name = "reps_alvo", nullable = false)
    private Integer repsAlvo;

    protected FichaExercicio() {
    }

    public FichaExercicio(Ficha ficha, Exercicio exercicio, Integer ordem, Integer seriesAlvo, Integer repsAlvo) {
        this.ficha = ficha;
        this.exercicio = exercicio;
        this.ordem = ordem;
        this.seriesAlvo = seriesAlvo;
        this.repsAlvo = repsAlvo;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Ficha getFicha() {
        return ficha;
    }

    public void setFicha(Ficha ficha) {
        this.ficha = ficha;
    }

    public Exercicio getExercicio() {
        return exercicio;
    }

    public void setExercicio(Exercicio exercicio) {
        this.exercicio = exercicio;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public Integer getSeriesAlvo() {
        return seriesAlvo;
    }

    public void setSeriesAlvo(Integer seriesAlvo) {
        this.seriesAlvo = seriesAlvo;
    }

    public Integer getRepsAlvo() {
        return repsAlvo;
    }

    public void setRepsAlvo(Integer repsAlvo) {
        this.repsAlvo = repsAlvo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FichaExercicio other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
