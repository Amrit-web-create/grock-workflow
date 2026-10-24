import { useEffect, useRef, useState } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const HOLD_MS = 280;

function digitAt(cell: number, now: number, slow: boolean): number {
  const step = Math.floor(now / (slow ? 1500 : 900));
  return (step + cell * 3 + 1) % 10;
}

export function CipherLock({ nonce, mode, onCommit, onDraft, onReject }: LockProps) {
  const [now, setNow] = useState(() => Date.now());
  const [slow, setSlow] = useState(false);
  const [seq, setSeq] = useState<number[]>([]);
  const [hold, setHold] = useState<{ cell: number; digit: number; started: number } | null>(null);
  const [progress, setProgress] = useState(0);
  const seqRef = useRef<number[]>([]);

  useEffect(() => {
    seqRef.current = [];
    setSeq([]);
    setHold(null);
    setProgress(0);
    onDraft(null);
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    setSlow(media.matches);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  useEffect(() => {
    if (hold) return;
    const id = window.setInterval(() => setNow(Date.now()), slow ? 200 : 120);
    return () => window.clearInterval(id);
  }, [hold, slow]);

  useEffect(() => {
    if (!hold) return;
    let frame = 0;
    const loop = () => {
      const amount = Math.min(1, (Date.now() - hold.started) / HOLD_MS);
      setProgress(amount);
      if (amount < 1) frame = requestAnimationFrame(loop);
    };
    frame = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(frame);
  }, [hold]);

  function pointerDown(cell: number, event: React.PointerEvent<HTMLButtonElement>) {
    if (seqRef.current.length >= 4) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    const digit = digitAt(cell, Date.now(), slow);
    setHold({ cell, digit, started: Date.now() });
    setProgress(0);
  }

  function pointerUp() {
    if (!hold) return;
    const amount = Math.min(1, (Date.now() - hold.started) / HOLD_MS);
    if (amount < 1) {
      onReject("Hold until the ring closes.");
      setHold(null);
      setProgress(0);
      return;
    }
    const next = [...seqRef.current, hold.digit].slice(0, 4);
    seqRef.current = next;
    setSeq(next);
    setHold(null);
    setProgress(0);
    tick(10);
    onDraft(next.join(""));
    if (next.length === 4) onCommit(next.join(""));
  }

  return (
    <div className="lock-ui flex flex-col items-center gap-4">
      <p className="text-sm text-mist">
        {mode === "edit" ? "Hold a numeral to keep it." : "Same four numerals. The grid will not sit still."}
      </p>
      <div className="grid grid-cols-3 gap-3">
        {Array.from({ length: 9 }, (_, cell) => {
          const digit = hold?.cell === cell ? hold.digit : digitAt(cell, now, slow);
          const active = hold?.cell === cell;
          return (
            <button
              key={cell}
              type="button"
              aria-label={`Cipher cell ${cell + 1}, showing ${digit}`}
              onPointerDown={(event) => pointerDown(cell, event)}
              onPointerUp={pointerUp}
              onPointerCancel={() => {
                setHold(null);
                setProgress(0);
              }}
              className="gesture relative flex size-16 items-center justify-center rounded-full bg-panel text-ivory"
            >
              <span
                className="pointer-events-none absolute inset-0 rounded-full"
                style={{
                  background: active
                    ? `conic-gradient(var(--color-ivory) ${Math.round(progress * 360)}deg, transparent 0deg)`
                    : undefined,
                  opacity: 0.9,
                }}
              />
              <span className="pointer-events-none absolute inset-1 rounded-full bg-panel" />
              <span className="font-display relative text-2xl leading-none">{digit}</span>
            </button>
          );
        })}
      </div>
      <div className="flex gap-2" aria-hidden="true">
        {Array.from({ length: 4 }, (_, i) => (
          <span
            key={i}
            className={`size-2 rounded-full ${i < seq.length ? "bg-ivory" : "bg-panel"}`}
          />
        ))}
      </div>
      {mode === "edit" && seq.length > 0 ? (
        <p className="font-display text-xl tracking-widest text-ivory">{seq.join(" · ")}</p>
      ) : null}
    </div>
  );
}
