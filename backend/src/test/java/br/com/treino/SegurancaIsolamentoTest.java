package br.com.treino;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Isolamento por usuário e autenticação via HTTP (MockMvc + banco real no container).
// E-mails únicos por teste dispensam limpeza entre métodos (cada classe tem seu banco).
@AutoConfigureMockMvc
class SegurancaIsolamentoTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void recursoDeOutroUsuarioResponde404() throws Exception {
        String emailA = "a." + UUID.randomUUID() + "@teste.com";
        String emailB = "b." + UUID.randomUUID() + "@teste.com";
        String tokenA = registrarELogar(emailA);
        String tokenB = registrarELogar(emailB);

        String exercicioId = criarExercicio(tokenA, "Supino Teste ISO");
        String fichaId = criarFicha(tokenA, exercicioId);
        String treinoId = criarTreino(tokenA, exercicioId);

        // Dono enxerga tudo.
        mvc.perform(get("/treinos/" + treinoId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Outro usuário recebe 404 (não 403, para não vazar existência).
        mvc.perform(get("/treinos/" + treinoId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mvc.perform(get("/fichas/" + fichaId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        // Vale para exercício criado por A; os globais do seed são visíveis a todos por desenho.
        mvc.perform(get("/exercicios/" + exercicioId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void requisicaoSemTokenResponde401() throws Exception {
        mvc.perform(get("/treinos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/exercicios")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginComCredenciaisErradasResponde401() throws Exception {
        String email = "login." + UUID.randomUUID() + "@teste.com";
        registrarELogar(email);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inexistente." + UUID.randomUUID() + "@teste.com\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String registrarELogar(String email) throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuario\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated());
        String resposta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("token").asText();
    }

    private String criarExercicio(String token, String nome) throws Exception {
        String resposta = mvc.perform(post("/exercicios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"grupoMuscular\":\"peito\",\"equipamento\":\"barra\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("id").asText();
    }

    private String criarFicha(String token, String exercicioId) throws Exception {
        String resposta = mvc.perform(post("/fichas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ficha A\",\"itens\":[{\"exercicioId\":\""
                                + exercicioId + "\",\"ordem\":1,\"seriesAlvo\":3,\"repsAlvo\":10}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("id").asText();
    }

    private String criarTreino(String token, String exercicioId) throws Exception {
        String ontem = LocalDate.now().minusDays(1).toString();
        String resposta = mvc.perform(post("/treinos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"" + ontem + "\",\"duracaoMin\":60,\"series\":[{"
                                + "\"exercicioId\":\"" + exercicioId + "\",\"numero\":1,"
                                + "\"repeticoes\":10,\"cargaKg\":80.00}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("id").asText();
    }
}
