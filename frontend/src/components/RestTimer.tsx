import { useEffect, useRef, useState } from "react";

const PRESETS = [30, 60, 90, 120];

function formatar(segundos: number): string {
  const m = Math.floor(segundos / 60);
  const s = segundos % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

// Beep via WebAudio + vibração ao fim do descanso.
function apitar() {
  try {
    const Janela = window as unknown as {
      AudioContext?: typeof AudioContext;
      webkitAudioContext?: typeof AudioContext;
    };
    const Ctor = Janela.AudioContext ?? Janela.webkitAudioContext;
    if (!Ctor) return;
    const ctx = new Ctor();
    const osc = ctx.createOscillator();
    const ganho = ctx.createGain();
    osc.connect(ganho);
    ganho.connect(ctx.destination);
    osc.frequency.value = 880;
    osc.start();
    ganho.gain.setValueAtTime(0.001, ctx.currentTime);
    ganho.gain.exponentialRampToValueAtTime(1, ctx.currentTime + 0.02);
    osc.stop(ctx.currentTime + 0.6);
    osc.onended = () => {
      void ctx.close();
    };
  } catch {
    // Áudio indisponível: o timer visual continua valendo.
  }
}

function vibrar() {
  try {
    if ("vibrate" in navigator) navigator.vibrate(300);
  } catch {
    // Vibração indisponível: ignora.
  }
}

export function RestTimer() {
  const [duracao, setDuracao] = useState(60);
  const [restantes, setRestantes] = useState(60);
  const [rodando, setRodando] = useState(false);
  const intervalo = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (intervalo.current !== null) window.clearInterval(intervalo.current);
    };
  }, []);

  useEffect(() => {
    if (!rodando) return;
    intervalo.current = window.setInterval(() => {
      setRestantes((r) => Math.max(0, r - 1));
    }, 1000);
    return () => {
      if (intervalo.current !== null) window.clearInterval(intervalo.current);
    };
  }, [rodando]);

  // Efeito separado do tick: dispara uma vez ao zerar.
  useEffect(() => {
    if (rodando && restantes === 0) {
      setRodando(false);
      apitar();
      vibrar();
    }
  }, [rodando, restantes]);

  function escolherPreset(segundos: number) {
    setRodando(false);
    setDuracao(segundos);
    setRestantes(segundos);
  }

  function zerar() {
    setRodando(false);
    setRestantes(duracao);
  }

  return (
    <section className="card timer" aria-label="Timer de descanso">
      <h2>Descanso</h2>
      <div className="timer-presets" role="group" aria-label="Duração do descanso">
        {PRESETS.map((p) => (
          <button
            key={p}
            type="button"
            className={duracao === p ? "" : "secundario"}
            onClick={() => escolherPreset(p)}
          >
            {p}s
          </button>
        ))}
      </div>
      <p className="timer-display" aria-live="polite">
        {formatar(restantes)}
      </p>
      <div className="acoes">
        {rodando ? (
          <button type="button" className="secundario" onClick={() => setRodando(false)}>
            Pausar
          </button>
        ) : (
          <button
            type="button"
            onClick={() => {
              if (restantes === 0) setRestantes(duracao);
              setRodando(true);
            }}
          >
            Iniciar
          </button>
        )}
        <button type="button" className="secundario" onClick={zerar}>
          Zerar
        </button>
      </div>
    </section>
  );
}
