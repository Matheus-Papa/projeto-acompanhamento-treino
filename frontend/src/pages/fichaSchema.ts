import { z } from "zod";

// Schema de ficha compartilhado por FichasPage (criar) e FichaDetalhePage (editar).
export const fichaItemSchema = z.object({
  exercicioId: z.string().min(1, "Exercício é obrigatório"),
  ordem: z.coerce.number().int().positive("Ordem deve ser maior que zero"),
  seriesAlvo: z.coerce.number().int().positive("Séries alvo devem ser maiores que zero"),
  repsAlvo: z.coerce.number().int().positive("Repetições alvo devem ser maiores que zero"),
});

export const fichaSchema = z.object({
  nome: z.string().min(1, "Nome é obrigatório").max(120, "Nome deve ter no máximo 120 caracteres"),
  itens: z.array(fichaItemSchema).min(1, "Ficha deve ter ao menos um exercício"),
});

export type FichaForm = z.infer<typeof fichaSchema>;
