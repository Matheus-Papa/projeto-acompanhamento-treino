import { z } from "zod";

// Campo vazio ("") vira undefined antes do coerce, com mensagem em português.
function numeroPositivo(mensagem: string) {
  return z.preprocess(
    (v) => (v === "" || v === null || v === undefined ? undefined : v),
    z.coerce.number({ invalid_type_error: "Informe um número válido" }).int("Informe um número inteiro").positive(mensagem),
  );
}

// Schema de ficha compartilhado por FichasPage (criar) e FichaDetalhePage (editar).
export const fichaItemSchema = z.object({
  exercicioId: z.string().min(1, "Exercício é obrigatório"),
  ordem: numeroPositivo("Ordem deve ser maior que zero"),
  seriesAlvo: numeroPositivo("Séries alvo devem ser maiores que zero"),
  repsAlvo: numeroPositivo("Repetições alvo devem ser maiores que zero"),
});

export const fichaSchema = z.object({
  nome: z.string().min(1, "Nome é obrigatório").max(120, "Nome deve ter no máximo 120 caracteres"),
  itens: z.array(fichaItemSchema).min(1, "Ficha deve ter ao menos um exercício"),
});

export type FichaForm = z.input<typeof fichaSchema>;
