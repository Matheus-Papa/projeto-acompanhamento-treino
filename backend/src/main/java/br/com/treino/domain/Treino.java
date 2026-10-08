package br.com.treino.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "treino")
public class Treino {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_treino_usuario"))
    private Usuario usuario;

    // Ficha de origem (opcional); se a ficha for excluída o treino permanece (SET NULL no banco).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ficha_id", foreignKey = @ForeignKey(name = "fk_treino_ficha"))
    private Ficha ficha;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "duracao_min")
    private Integer duracaoMin;

    @Column(name = "observacao", length = 500)
    private String observacao;

    // Transação única no service: persistir o treino persiste as séries; rollback apaga tudo.
    @OneToMany(mappedBy = "treino", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Serie> series = new ArrayList<>();

    protected Treino() {
    }

    public Treino(Usuario usuario, Ficha ficha, LocalDate data, Integer duracaoMin, String observacao) {
        this.usuario = usuario;
        this.ficha = ficha;
        this.data = data;
        this.duracaoMin = duracaoMin;
        this.observacao = observacao;
    }

    public void addSerie(Serie serie) {
        series.add(serie);
        serie.setTreino(this);
    }

    public void removeSerie(Serie serie) {
        series.remove(serie);
        serie.setTreino(null);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Ficha getFicha() {
        return ficha;
    }

    public void setFicha(Ficha ficha) {
        this.ficha = ficha;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Integer getDuracaoMin() {
        return duracaoMin;
    }

    public void setDuracaoMin(Integer duracaoMin) {
        this.duracaoMin = duracaoMin;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<Serie> getSeries() {
        return series;
    }

    public void setSeries(List<Serie> series) {
        this.series = series;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Treino other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
