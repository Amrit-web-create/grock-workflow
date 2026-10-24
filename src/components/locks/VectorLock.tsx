import { useEffect, useRef, useState } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

type Dir = "U" | "D" | "L" | "R";

function classify(dx: number, dy: number): Dir | null {
  const dist = Math.hypot(dx, dy);
  if (dist < 52) return null;
  if (Math.abs(dx) > Math.abs(dy) * 1.4) return dx > 0 ? "R" : "L";
  if (Math.abs(dy) > Math.abs(dx) * 1.4) return dy > 0 ? "D" : "U";
  return null;
}

const WORD: Record<Dir, string> = { U: "Up", D: "Down", L: "Left", R: "Right" };

export function VectorLock({ nonce, mode, onCommit, onDraft, onReject }: LockProps) {
  const [path, setPath] = useState<Dir[]>([]);
  const pathRef = useRef<Dir[]>([]);
  const start = useRef<{ x: number; y: number } | null>(null);

  useEffect(() => {
    pathRef.current = [];
    setPath([]);
    onDraft(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function down(event: React.PointerEvent<HTMLDivElement>) {
    if (pathRef.current.length >= 5) return;
    start.current = { x: event.clientX, y: event.clientY };
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function up(event: React.PointerEvent<HTMLDivElement>) {
    if (!start.current) return;
    const dir = classify(event.clientX - start.current.x, event.clientY - start.current.y);
    start.current = null;
    if (!dir) {
      onReject("Swipe straight. Diagonals don’t count.");
      return;
    }
    const next = [...pathRef.current, dir];
    pathRef.current = next;
    setPath(next);
    onDraft(next.join("-"));
    tick(10);
    if (next.length === 5) onCommit(next.join("-"));
  }

  return (
    <div className="lock-ui flex w-full max-w-sm flex-col items-center gap-3">
      <p className="text-sm text-mist">
        {mode === "edit" ? "Five straight swipes." : "Repeat the five directions."}
      </p>
      <div
        className="gesture flex h-56 w-full items-center justify-center rounded-3xl border border-ivory/10 bg-panel"
        onPointerDown={down}
        onPointerUp={up}
        onPointerCancel={() => {
          start.current = null;
        }}
        role="application"
        aria-label="Swipe pad"
      >
        <span className="text-sm text-mist">{path.length ? `${path.length} of 5` : "Swipe"}</span>
      </div>
      <div className="flex min-h-6 gap-2 text-sm text-ivory">
        {path.map((dir, index) => (
          <span key={`${dir}-${index}`}>{mode === "edit" ? WORD[dir] : "•"}</span>
        ))}
      </div>
      {path.length > 0 ? (
        <button
          type="button"
          className="text-sm text-mist"
          onClick={() => {
            pathRef.current = [];
            setPath([]);
            onDraft(null);
          }}
        >
          Clear path
        </button>
      ) : null}
    </div>
  );
}
