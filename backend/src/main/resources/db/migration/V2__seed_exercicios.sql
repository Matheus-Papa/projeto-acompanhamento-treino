-- V2__seed_exercicios.sql — 30 exercícios globais (usuario_id NULL).
-- Idempotente: insere só os nomes globais ainda inexistentes; ids fixos
-- para que reexecuções manuais não dupliquem linhas.

INSERT INTO exercicio (id, nome, grupo_muscular, equipamento)
SELECT v.id, v.nome, v.grupo_muscular, v.equipamento
FROM (VALUES
    ('11111111-1111-4111-8111-000000000001'::uuid, 'Supino reto com barra', 'peito', 'barra'),
    ('11111111-1111-4111-8111-000000000002'::uuid, 'Supino inclinado com halteres', 'peito', 'halter'),
    ('11111111-1111-4111-8111-000000000003'::uuid, 'Supino declinado com barra', 'peito', 'barra'),
    ('11111111-1111-4111-8111-000000000004'::uuid, 'Crucifixo com halteres', 'peito', 'halter'),
    ('11111111-1111-4111-8111-000000000005'::uuid, 'Crossover no cabo', 'peito', 'cabo'),
    ('11111111-1111-4111-8111-000000000006'::uuid, 'Levantamento terra', 'costas', 'barra'),
    ('11111111-1111-4111-8111-000000000007'::uuid, 'Barra fixa', 'costas', 'peso corporal'),
    ('11111111-1111-4111-8111-000000000008'::uuid, 'Remada curvada com barra', 'costas', 'barra'),
    ('11111111-1111-4111-8111-000000000009'::uuid, 'Remada unilateral com halter', 'costas', 'halter'),
    ('11111111-1111-4111-8111-000000000010'::uuid, 'Puxada alta na maquina', 'costas', 'maquina'),
    ('11111111-1111-4111-8111-000000000011'::uuid, 'Agachamento livre', 'pernas', 'barra'),
    ('11111111-1111-4111-8111-000000000012'::uuid, 'Leg press 45', 'pernas', 'maquina'),
    ('11111111-1111-4111-8111-000000000013'::uuid, 'Cadeira extensora', 'pernas', 'maquina'),
    ('11111111-1111-4111-8111-000000000014'::uuid, 'Mesa flexora', 'pernas', 'maquina'),
    ('11111111-1111-4111-8111-000000000015'::uuid, 'Levantamento terra romeno', 'pernas', 'barra'),
    ('11111111-1111-4111-8111-000000000016'::uuid, 'Afundo com halteres', 'pernas', 'halter'),
    ('11111111-1111-4111-8111-000000000017'::uuid, 'Desenvolvimento com barra', 'ombros', 'barra'),
    ('11111111-1111-4111-8111-000000000018'::uuid, 'Desenvolvimento com halteres', 'ombros', 'halter'),
    ('11111111-1111-4111-8111-000000000019'::uuid, 'Elevacao lateral com halteres', 'ombros', 'halter'),
    ('11111111-1111-4111-8111-000000000020'::uuid, 'Face pull no cabo', 'ombros', 'cabo'),
    ('11111111-1111-4111-8111-000000000021'::uuid, 'Rosca direta com barra', 'biceps', 'barra'),
    ('11111111-1111-4111-8111-000000000022'::uuid, 'Rosca alternada com halteres', 'biceps', 'halter'),
    ('11111111-1111-4111-8111-000000000023'::uuid, 'Rosca no cabo', 'biceps', 'cabo'),
    ('11111111-1111-4111-8111-000000000024'::uuid, 'Triceps testa com barra', 'triceps', 'barra'),
    ('11111111-1111-4111-8111-000000000025'::uuid, 'Triceps pulley no cabo', 'triceps', 'cabo'),
    ('11111111-1111-4111-8111-000000000026'::uuid, 'Mergulho em paralelas', 'triceps', 'peso corporal'),
    ('11111111-1111-4111-8111-000000000027'::uuid, 'Prancha isometrica', 'core', 'peso corporal'),
    ('11111111-1111-4111-8111-000000000028'::uuid, 'Abdominal crunch', 'core', 'peso corporal'),
    ('11111111-1111-4111-8111-000000000029'::uuid, 'Elevacao de pernas na barra fixa', 'core', 'peso corporal'),
    ('11111111-1111-4111-8111-000000000030'::uuid, 'Abdominal na maquina', 'core', 'maquina')
) AS v (id, nome, grupo_muscular, equipamento)
WHERE NOT EXISTS (
    SELECT 1
    FROM exercicio e
    WHERE e.usuario_id IS NULL
      AND e.nome = v.nome
);
