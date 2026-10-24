import { useEffect, useRef, useState } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const TONES = [
  { id: "ink", className: "bg-tone-ink" },
  { id: "teal", className: "bg-tone-teal" },
  { id: "sand", className: "bg-tone-sand" },
  { id: "leaf", className: "bg-tone-leaf" },
  { id: "clay", className: "bg-tone-clay" },
  { id: "dusk", className: "bg-tone-dusk" },
] as const;

export function ChromaLock({ nonce, mode, onCommit, onDraft }: LockProps) {
  const [seq, setSeq] = useState<string[]>([]);
  const [pop, setPop] = useState<string | null>(null);
  const seqRef = useRef<string[]>([]);

  useEffect(() => {
    seqRef.current = [];
    setSeq([]);
    setPop(null);
    onDraft(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function tap(id: string) {
    if (seqRef.current.length >= 5) return;
    const next = [...seqRef.current, id];
    seqRef.current = next;
    setSeq(next);
    setPop(id + next.length);
    onDraft(next.join("."));
    tick(10);
    if (next.length === 5) onCommit(next.join("."));
  }

  return (
    <div className="lock-ui flex flex-col items-center gap-5">
      <p className="text-sm text-mist">
        {mode === "edit" ? "Five tones. Order is the secret." : "Repeat the five tones."}
      </p>
      <div className="grid grid-cols-3 gap-4">
        {TONES.map((tone) => (
          <button
            key={tone.id}
            type="button"
            aria-label={tone.id}
            onClick={() => tap(tone.id)}
            className={`size-16 rounded-full ${tone.className} ${pop?.startsWith(tone.id) ? "pop" : ""}`}
          />
        ))}
      </div>
      <div className="flex h-8 items-center gap-2">
        {seq.map((id, index) => {
          const tone = TONES.find((item) => item.id === id);
          return <span key={`${id}-${index}`} className={`size-3 rounded-full ${tone?.className ?? ""}`} />;
        })}
        {mode === "challenge"
          ? Array.from({ length: Math.max(0, 5 - seq.length) }, (_, i) => (
              <span key={i} className="size-3 rounded-full bg-panel" />
            ))
          : null}
      </div>
    </div>
  );
}
