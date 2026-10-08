// Helpers de data compartilhados. O backend trafega datas como ISO yyyy-MM-dd.
export function hojeISO(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, "0");
  const dia = String(agora.getDate()).padStart(2, "0");
  return `${agora.getFullYear()}-${mes}-${dia}`;
}

export function formatarData(iso: string | null | undefined): string {
  if (!iso || typeof iso !== "string") return "—";
  const parte = iso.includes("T") ? iso.slice(0, 10) : iso;
  const [ano, mes, dia] = parte.split("-");
  if (!ano || !mes || !dia) return "—";
  return `${dia}/${mes}/${ano}`;
}
