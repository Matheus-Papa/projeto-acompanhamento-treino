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
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "serie")
public class Serie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treino_id", nullable = false, foreignKey = @ForeignKey(name = "fk_serie_treino"))
    private Treino treino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercicio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_serie_exercicio"))
    private Exercicio exercicio;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "repeticoes", nullable = false)
    private Integer repeticoes;

    @Column(name = "carga_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal cargaKg;

    @Column(name = "rpe")
    private Integer rpe;

    protected Serie() {
    }

    public Serie(Treino treino, Exercicio exercicio, Integer numero, Integer repeticoes, BigDecimal cargaKg, Integer rpe) {
        this.treino = treino;
        this.exercicio = exercicio;
        this.numero = numero;
        this.repeticoes = repeticoes;
        this.cargaKg = cargaKg;
        this.rpe = rpe;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Treino getTreino() {
        return treino;
    }

    public void setTreino(Treino treino) {
        this.treino = treino;
    }

    public Exercicio getExercicio() {
        return exercicio;
    }

    public void setExercicio(Exercicio exercicio) {
        this.exercicio = exercicio;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getRepeticoes() {
        return repeticoes;
    }

    public void setRepeticoes(Integer repeticoes) {
        this.repeticoes = repeticoes;
    }

    public BigDecimal getCargaKg() {
        return cargaKg;
    }

    public void setCargaKg(BigDecimal cargaKg) {
        this.cargaKg = cargaKg;
    }

    public Integer getRpe() {
        return rpe;
    }

    public void setRpe(Integer rpe) {
        this.rpe = rpe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Serie other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
