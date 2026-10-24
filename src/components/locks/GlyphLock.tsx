import { useEffect, useRef, useState } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const SHAPES = ["ring", "triangle", "bar", "cross", "diamond", "chevron"] as const;
type ShapeId = (typeof SHAPES)[number];
const CORNERS = ["nw", "ne", "sw", "se"] as const;

function ShapeMark({ id, light = false }: { id: ShapeId; light?: boolean }) {
  const stroke = light ? "var(--color-night)" : "var(--color-ivory)";
  return (
    <svg viewBox="0 0 48 48" className="size-8" aria-hidden="true">
      {id === "ring" ? <circle cx="24" cy="24" r="10" fill="none" stroke={stroke} strokeWidth="3" /> : null}
      {id === "triangle" ? (
        <path d="M24 10 L38 36 H10 Z" fill="none" stroke={stroke} strokeWidth="3" />
      ) : null}
      {id === "bar" ? <path d="M10 24 H38" stroke={stroke} strokeWidth="4" strokeLinecap="round" /> : null}
      {id === "cross" ? (
        <path d="M24 10 V38 M10 24 H38" stroke={stroke} strokeWidth="3" strokeLinecap="round" />
      ) : null}
      {id === "diamond" ? (
        <path d="M24 8 L40 24 L24 40 L8 24 Z" fill="none" stroke={stroke} strokeWidth="3" />
      ) : null}
      {id === "chevron" ? (
        <path d="M12 18 L24 32 L36 18" fill="none" stroke={stroke} strokeWidth="3" strokeLinecap="round" />
      ) : null}
    </svg>
  );
}

export function GlyphLock({ nonce, onCommit, onDraft, onReject }: LockProps) {
  const [placed, setPlaced] = useState<Array<ShapeId | null>>([null, null, null, null]);
  const [drag, setDrag] = useState<{ id: ShapeId; x: number; y: number } | null>(null);
  const docks = useRef<Array<HTMLDivElement | null>>([]);
  const placedRef = useRef(placed);
  placedRef.current = placed;

  useEffect(() => {
    setPlaced([null, null, null, null]);
    setDrag(null);
    onDraft(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function publish(next: Array<ShapeId | null>) {
    if (next.every(Boolean)) onDraft(next.join("."));
    else onDraft(null);
  }

  function down(id: ShapeId, event: React.PointerEvent<HTMLButtonElement>) {
    if (placedRef.current.includes(id)) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    setDrag({ id, x: event.clientX, y: event.clientY });
  }

  function move(event: React.PointerEvent<HTMLButtonElement>) {
    setDrag((current) => (current ? { ...current, x: event.clientX, y: event.clientY } : current));
  }

  function up(event: React.PointerEvent<HTMLButtonElement>) {
    const current = drag;
    setDrag(null);
    if (!current) return;
    const hit = docks.current.findIndex((node) => {
      if (!node) return false;
      const rect = node.getBoundingClientRect();
      return (
        event.clientX >= rect.left &&
        event.clientX <= rect.right &&
        event.clientY >= rect.top &&
        event.clientY <= rect.bottom
      );
    });
    if (hit < 0) {
      onReject("Drop the shape into an empty dock.");
      return;
    }
    if (placedRef.current[hit]) {
      onReject("That dock is already filled.");
      return;
    }
    const next = placedRef.current.slice();
    next[hit] = current.id;
    setPlaced(next);
    publish(next);
    tick(10);
  }

  function clearDock(index: number) {
    const next = placed.slice();
    next[index] = null;
    setPlaced(next);
    publish(next);
  }

  function seal(corner: (typeof CORNERS)[number]) {
    if (placed.some((item) => !item)) {
      onReject("Fill all four docks first.");
      return;
    }
    tick(12);
    onCommit(`${placed.join(".")}@${corner}`);
  }

  const tray = SHAPES.filter((id) => !placed.includes(id));
  const ready = placed.every(Boolean);

  return (
    <div className="lock-ui flex flex-col items-center gap-4">
      <p className="text-sm text-mist">Drag four shapes in, then tap a corner.</p>
      <div className="grid grid-cols-4 gap-2">
        {placed.map((id, index) => (
          <div
            key={index}
            ref={(node) => {
              docks.current[index] = node;
            }}
            className="flex size-16 items-center justify-center rounded-2xl border border-ivory/15 bg-panel"
          >
            {id ? (
              <button type="button" aria-label={`Remove ${id}`} onClick={() => clearDock(index)}>
                <ShapeMark id={id} />
              </button>
            ) : (
              <span className="text-xs text-mist">{index + 1}</span>
            )}
          </div>
        ))}
      </div>
      <div className="flex flex-wrap justify-center gap-2">
        {tray.map((id) => (
          <button
            key={id}
            type="button"
            aria-label={id}
            onPointerDown={(event) => down(id, event)}
            onPointerMove={move}
            onPointerUp={up}
            onPointerCancel={() => setDrag(null)}
            className="gesture flex size-14 items-center justify-center rounded-2xl bg-ivory"
          >
            <ShapeMark id={id} light />
          </button>
        ))}
      </div>
      {ready ? (
        <div className="grid grid-cols-2 gap-2">
          {CORNERS.map((corner) => (
            <button
              key={corner}
              type="button"
              onClick={() => seal(corner)}
              className="h-11 rounded-full bg-ivory px-4 text-sm font-semibold text-night"
            >
              Seal {corner.toUpperCase()}
            </button>
          ))}
        </div>
      ) : null}
      {drag ? (
        <div
          className="pointer-events-none fixed z-50 flex size-14 items-center justify-center rounded-2xl bg-ivory"
          style={{ left: drag.x - 28, top: drag.y - 28 }}
        >
          <ShapeMark id={drag.id} light />
        </div>
      ) : null}
    </div>
  );
}
