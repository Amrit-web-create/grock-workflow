import { useEffect, useRef } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const ITEM = 64;

function wrap(n: number): number {
  return ((n % 10) + 10) % 10;
}

function Wheel({
  label,
  onDigit,
}: {
  label: string;
  onDigit: (n: number) => void;
}) {
  const pos = useRef(0);
  const vel = useRef(0);
  const drag = useRef(false);
  const lastY = useRef(0);
  const lastT = useRef(0);
  const lastDigit = useRef(0);
  const track = useRef<HTMLDivElement>(null);
  const cells = useRef<Array<HTMLDivElement | null>>([]);
  const onDigitRef = useRef(onDigit);
  onDigitRef.current = onDigit;

  function paint() {
    const node = track.current;
    if (!node) return;
    let p = pos.current;
    p = ((p % 10) + 10) % 10;
    pos.current = p;
    const base = Math.floor(p);
    const frac = p - base;
    node.style.transform = `translateY(${ITEM - frac * ITEM - 3 * ITEM}px)`;
    for (let i = 0; i < 7; i += 1) {
      const el = cells.current[i];
      if (!el) continue;
      const digit = wrap(base + (i - 3));
      el.textContent = String(digit);
      const dist = Math.abs(i - 3 - frac);
      el.style.opacity = String(Math.max(0.28, 1 - dist * 0.38));
    }
  }

  function publish() {
    const shown = wrap(Math.round(pos.current));
    if (shown !== lastDigit.current) {
      lastDigit.current = shown;
      onDigitRef.current(shown);
      tick(drag.current ? 4 : 8);
    }
  }

  useEffect(() => {
    paint();
    let frame = 0;
    let alive = true;
    const loop = () => {
      if (!alive) return;
      if (!drag.current) {
        pos.current += vel.current;
        vel.current *= 0.9;
        if (Math.abs(vel.current) < 0.015) {
          const p = ((pos.current % 10) + 10) % 10;
          const target = wrap(Math.round(p));
          let delta = target - p;
          if (delta > 5) delta -= 10;
          if (delta < -5) delta += 10;
          pos.current += delta * 0.28;
          if (Math.abs(delta) < 0.012) {
            pos.current = target;
            vel.current = 0;
          }
        }
        publish();
      }
      paint();
      frame = requestAnimationFrame(loop);
    };
    frame = requestAnimationFrame(loop);
    return () => {
      alive = false;
      cancelAnimationFrame(frame);
    };
    // Paint loop owns the DOM.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function down(event: React.PointerEvent<HTMLDivElement>) {
    drag.current = true;
    lastY.current = event.clientY;
    lastT.current = performance.now();
    vel.current = 0;
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function move(event: React.PointerEvent<HTMLDivElement>) {
    if (!drag.current) return;
    const dy = event.clientY - lastY.current;
    const dt = Math.max(8, performance.now() - lastT.current);
    lastY.current = event.clientY;
    lastT.current = performance.now();
    pos.current -= dy / ITEM;
    vel.current = (-dy / ITEM) * (16 / dt) * 0.9;
    publish();
    paint();
  }

  function up() {
    drag.current = false;
  }

  function nudge(dir: number) {
    const target = wrap(lastDigit.current + dir);
    pos.current = target;
    vel.current = 0;
    lastDigit.current = target;
    onDigitRef.current(target);
    paint();
    tick(8);
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <span className="text-xs uppercase tracking-widest text-mist">{label}</span>
      <div
        className="gesture relative h-48 w-16 overflow-hidden rounded-2xl bg-panel"
        onPointerDown={down}
        onPointerMove={move}
        onPointerUp={up}
        onPointerCancel={up}
        role="slider"
        aria-valuemin={0}
        aria-valuemax={9}
        aria-valuenow={lastDigit.current}
        aria-label={label}
        tabIndex={0}
        onKeyDown={(event) => {
          if (event.key === "ArrowUp") nudge(-1);
          if (event.key === "ArrowDown") nudge(1);
        }}
      >
        <div className="pointer-events-none absolute inset-x-0 top-16 h-16 border-y border-ivory/20" />
        <div ref={track} className="absolute inset-x-0 top-0">
          {Array.from({ length: 7 }, (_, i) => (
            <div
              key={i}
              ref={(el) => {
                cells.current[i] = el;
              }}
              className="flex h-16 items-center justify-center font-display text-4xl text-ivory"
            />
          ))}
        </div>
      </div>
    </div>
  );
}

export function WheelLock({ nonce, onDraft }: LockProps) {
  const digits = useRef([0, 0, 0]);

  useEffect(() => {
    digits.current = [0, 0, 0];
    onDraft("000");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function setDigit(index: number, value: number) {
    const next = digits.current.slice();
    next[index] = value;
    digits.current = next;
    onDraft(next.join(""));
  }

  return (
    <div className="lock-ui flex flex-col items-center gap-4">
      <p className="text-sm text-mist">Drag with weight. Arrow keys work too.</p>
      <div className="flex gap-3">
        <Wheel key={`a-${nonce}`} label="One" onDigit={(n) => setDigit(0, n)} />
        <Wheel key={`b-${nonce}`} label="Two" onDigit={(n) => setDigit(1, n)} />
        <Wheel key={`c-${nonce}`} label="Three" onDigit={(n) => setDigit(2, n)} />
      </div>
    </div>
  );
}
