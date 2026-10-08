-- V1__create_schema.sql — esquema inicial do acompanhamento de treino.
-- Convenções: PKs UUID geradas na aplicação (GenerationType.UUID),
-- FKs nomeadas, CHECKs de domínio, índices de isolamento (usuario_id) e histórico.

CREATE TABLE IF NOT EXISTS usuario (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS exercicio (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    grupo_muscular VARCHAR(60) NOT NULL,
    equipamento VARCHAR(60),
    usuario_id UUID,
    CONSTRAINT fk_exercicio_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_exercicio_usuario ON exercicio (usuario_id);

CREATE TABLE IF NOT EXISTS ficha (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    usuario_id UUID NOT NULL,
    CONSTRAINT fk_ficha_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ficha_usuario ON ficha (usuario_id);

CREATE TABLE IF NOT EXISTS ficha_exercicio (
    id UUID PRIMARY KEY,
    ficha_id UUID NOT NULL,
    exercicio_id UUID NOT NULL,
    ordem INT NOT NULL CHECK (ordem > 0),
    series_alvo INT NOT NULL CHECK (series_alvo > 0),
    reps_alvo INT NOT NULL CHECK (reps_alvo > 0),
    CONSTRAINT fk_fichaexercicio_ficha FOREIGN KEY (ficha_id)
        REFERENCES ficha (id) ON DELETE CASCADE,
    CONSTRAINT fk_fichaexercicio_exercicio FOREIGN KEY (exercicio_id)
        REFERENCES exercicio (id) ON DELETE RESTRICT,
    CONSTRAINT uq_fichaexercicio_ficha_ordem UNIQUE (ficha_id, ordem)
);
CREATE INDEX IF NOT EXISTS idx_fichaexercicio_ficha ON ficha_exercicio (ficha_id);

CREATE TABLE IF NOT EXISTS treino (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    ficha_id UUID,
    data DATE NOT NULL,
    duracao_min INT CHECK (duracao_min IS NULL OR duracao_min > 0),
    observacao VARCHAR(500),
    CONSTRAINT fk_treino_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_treino_ficha FOREIGN KEY (ficha_id)
        REFERENCES ficha (id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_treino_usuario_data ON treino (usuario_id, data);

CREATE TABLE IF NOT EXISTS serie (
    id UUID PRIMARY KEY,
    treino_id UUID NOT NULL,
    exercicio_id UUID NOT NULL,
    numero INT NOT NULL CHECK (numero > 0),
    repeticoes INT NOT NULL CHECK (repeticoes > 0),
    carga_kg NUMERIC(6, 2) NOT NULL CHECK (carga_kg >= 0),
    rpe INT CHECK (rpe IS NULL OR (rpe >= 1 AND rpe <= 10)),
    CONSTRAINT fk_serie_treino FOREIGN KEY (treino_id)
        REFERENCES treino (id) ON DELETE CASCADE,
    CONSTRAINT fk_serie_exercicio FOREIGN KEY (exercicio_id)
        REFERENCES exercicio (id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_serie_treino ON serie (treino_id);
CREATE INDEX IF NOT EXISTS idx_serie_exercicio ON serie (exercicio_id);

CREATE TABLE IF NOT EXISTS medida_corporal (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    data DATE NOT NULL,
    peso_kg NUMERIC(6, 2) NOT NULL CHECK (peso_kg > 0),
    percentual_gordura NUMERIC(5, 2)
        CHECK (percentual_gordura IS NULL OR (percentual_gordura >= 0 AND percentual_gordura <= 100)),
    CONSTRAINT fk_medida_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_medida_usuario_data ON medida_corporal (usuario_id, data);
