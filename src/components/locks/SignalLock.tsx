import { useEffect, useMemo, useRef, useState } from "react";
import {
  Compass,
  Eye,
  Hexagon,
  KeyRound,
  Moon,
  Shield,
  Star,
  Sun,
  Zap,
} from "lucide-react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const SYMBOLS = [
  { id: "shield", Icon: Shield },
  { id: "key", Icon: KeyRound },
  { id: "eye", Icon: Eye },
  { id: "bolt", Icon: Zap },
  { id: "moon", Icon: Moon },
  { id: "sun", Icon: Sun },
  { id: "star", Icon: Star },
  { id: "hex", Icon: Hexagon },
  { id: "compass", Icon: Compass },
] as const;

type SymbolId = (typeof SYMBOLS)[number]["id"];

function mix(seed: number) {
  let a = seed || 1;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function shuffle(seed: number): SymbolId[] {
  const rand = mix(seed);
  const next: SymbolId[] = SYMBOLS.map((item) => item.id);
  for (let i = next.length - 1; i > 0; i -= 1) {
    const j = Math.floor(rand() * (i + 1));
    const swap = next[i] ?? next[0];
    next[i] = next[j] ?? swap;
    next[j] = swap;
  }
  return next;
}

export function SignalLock({ nonce, mode, onCommit, onDraft, onReject }: LockProps) {
  const order = useMemo(() => shuffle(nonce + 17), [nonce]);
  const [seq, setSeq] = useState<SymbolId[]>([]);
  const seqRef = useRef<SymbolId[]>([]);

  useEffect(() => {
    seqRef.current = [];
    setSeq([]);
    onDraft(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function tap(id: SymbolId) {
    if (seqRef.current.includes(id)) {
      onReject("Each symbol is used once.");
      return;
    }
    if (seqRef.current.length >= 4) return;
    const next = [...seqRef.current, id];
    seqRef.current = next;
    setSeq(next);
    onDraft(next.join("."));
    tick(8);
    if (next.length === 4) onCommit(next.join("."));
  }

  return (
    <div className="lock-ui flex flex-col items-center gap-4">
      <p className="max-w-xs text-center text-sm text-mist">
        {mode === "edit"
          ? "Remember the marks. Next time they will move."
          : "The grid moved. Tap the same marks, in order."}
      </p>
      <div className="grid grid-cols-3 gap-2">
        {order.map((id) => {
          const Icon = SYMBOLS.find((item) => item.id === id)?.Icon ?? Shield;
          const picked = seq.includes(id);
          return (
            <button
              key={id}
              type="button"
              aria-label={id}
              onClick={() => tap(id)}
              className={`flex size-16 items-center justify-center rounded-2xl ${
                picked ? "bg-ivory text-night" : "bg-panel text-ivory"
              }`}
            >
              <Icon size={22} strokeWidth={1.75} />
            </button>
          );
        })}
      </div>
      <div className="flex h-8 gap-2">
        {seq.map((id) => {
          const Icon = SYMBOLS.find((item) => item.id === id)?.Icon ?? Shield;
          return (
            <span key={id} className="text-ivory">
              <Icon size={16} />
            </span>
          );
        })}
      </div>
    </div>
  );
}
