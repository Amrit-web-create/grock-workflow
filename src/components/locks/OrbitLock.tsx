import { useEffect, useRef, useState } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const BUBBLES = [
  { n: 1, x: "12%", y: "18%", drift: "drift-a" },
  { n: 2, x: "48%", y: "8%", drift: "drift-b" },
  { n: 3, x: "74%", y: "20%", drift: "drift-c" },
  { n: 4, x: "18%", y: "52%", drift: "drift-d" },
  { n: 5, x: "58%", y: "46%", drift: "drift-a" },
  { n: 6, x: "78%", y: "58%", drift: "drift-b" },
  { n: 7, x: "30%", y: "76%", drift: "drift-c" },
  { n: 8, x: "64%", y: "74%", drift: "drift-d" },
] as const;

export function OrbitLock({ nonce, mode, onCommit, onDraft, onReject }: LockProps) {
  const [seq, setSeq] = useState<number[]>([]);
  const seqRef = useRef<number[]>([]);

  useEffect(() => {
    seqRef.current = [];
    setSeq([]);
    onDraft(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function tap(n: number) {
    if (seqRef.current.includes(n)) {
      onReject("That numeral is already in the order.");
      return;
    }
    if (seqRef.current.length >= 5) return;
    const next = [...seqRef.current, n];
    seqRef.current = next;
    setSeq(next);
    onDraft(next.join("-"));
    tick(8);
    if (next.length === 5) onCommit(next.join("-"));
  }

  return (
    <div className="lock-ui flex w-full max-w-sm flex-col items-center gap-3">
      <p className="text-sm text-mist">
        {mode === "edit" ? "Tap five numerals in the order you want." : "Same five, while they drift."}
      </p>
      <div className="relative h-72 w-full">
        {BUBBLES.map((bubble) => {
          const picked = seq.includes(bubble.n);
          return (
            <button
              key={bubble.n}
              type="button"
              aria-label={`Numeral ${bubble.n}`}
              onClick={() => tap(bubble.n)}
              className={`absolute flex size-14 items-center justify-center rounded-full ${bubble.drift} ${
                picked ? "bg-ivory text-night" : "bg-panel text-ivory"
              }`}
              style={{ left: bubble.x, top: bubble.y }}
            >
              <span className="font-display text-xl leading-none">{bubble.n}</span>
            </button>
          );
        })}
      </div>
      <p className="font-display text-lg tracking-widest text-ivory">
        {seq.length ? seq.join("  ") : "—"}
      </p>
    </div>
  );
}
