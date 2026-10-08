package br.com.treino;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Base dos testes de integração: sobe Postgres 16 em container por classe de teste
// e aponta o datasource + JWT para ele. O Flyway migra V1 (schema) + V2 (seed de
// 30 exercícios globais) automaticamente na subida do contexto.
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        // Mesmo valor de application-test.properties (>= 32 chars); o dinâmico prevalece.
        registro.add("app.jwt.secret", () -> "segredo-de-teste-com-mais-de-32-caracteres-0123456789");
    }
}
