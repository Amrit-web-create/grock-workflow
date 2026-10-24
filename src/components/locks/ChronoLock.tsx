import { useEffect, useRef, useState } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

export function ChronoLock({ nonce, onDraft }: LockProps) {
  const [hour, setHour] = useState(12);
  const [minute, setMinute] = useState(0);
  const [meridiem, setMeridiem] = useState<"AM" | "PM">("AM");
  const [hand, setHand] = useState<"hour" | "minute" | null>(null);
  const handRef = useRef<"hour" | "minute" | null>(null);
  const face = useRef<SVGSVGElement>(null);

  useEffect(() => {
    setHour(12);
    setMinute(0);
    setMeridiem("AM");
    onDraft("12:00:AM");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function publish(nextHour: number, nextMinute: number, nextMeridiem: "AM" | "PM") {
    onDraft(`${pad(nextHour)}:${pad(nextMinute)}:${nextMeridiem}`);
  }

  function angleOf(event: React.PointerEvent) {
    const node = face.current;
    if (!node) return 0;
    const rect = node.getBoundingClientRect();
    const dx = event.clientX - (rect.left + rect.width / 2);
    const dy = event.clientY - (rect.top + rect.height / 2);
    const deg = (Math.atan2(dy, dx) * 180) / Math.PI;
    return (deg + 90 + 360) % 360;
  }

  function apply(which: "hour" | "minute", event: React.PointerEvent) {
    const deg = angleOf(event);
    if (which === "minute") {
      const next = Math.round(deg / 6) % 60;
      setMinute(next);
      publish(hour, next, meridiem);
    } else {
      const raw = Math.round(deg / 30) % 12;
      const next = raw === 0 ? 12 : raw;
      setHour(next);
      publish(next, minute, meridiem);
    }
    tick(6);
  }

  function down(which: "hour" | "minute", event: React.PointerEvent) {
    event.currentTarget.setPointerCapture(event.pointerId);
    handRef.current = which;
    setHand(which);
    apply(which, event);
  }

  const hourAngle = (hour % 12) * 30;
  const minuteAngle = minute * 6;

  return (
    <div className="lock-ui flex flex-col items-center gap-4">
      <p className="font-display text-3xl text-ivory">
        {pad(hour)}:{pad(minute)}
        <span className="ml-2 font-sans text-sm tracking-widest text-mist">{meridiem}</span>
      </p>
      <svg
        ref={face}
        viewBox="0 0 240 240"
        className="gesture h-60 w-60"
        onPointerMove={(event) => {
          if (hand) apply(hand, event);
        }}
        onPointerUp={() => setHand(null)}
        onPointerLeave={() => setHand(null)}
      >
        <circle cx="120" cy="120" r="108" fill="var(--color-panel)" />
        {Array.from({ length: 12 }, (_, i) => {
          const a = ((i * 30 - 90) * Math.PI) / 180;
          const x = 120 + Math.cos(a) * 88;
          const y = 120 + Math.sin(a) * 88;
          return (
            <text
              key={i}
              x={x}
              y={y}
              textAnchor="middle"
              dominantBaseline="central"
              fill="var(--color-mist)"
              fontSize="11"
              fontFamily="Manrope, sans-serif"
            >
              {i === 0 ? 12 : i}
            </text>
          );
        })}
        <line
          x1="120"
          y1="120"
          x2="120"
          y2="72"
          stroke="var(--color-ivory)"
          strokeWidth="4"
          strokeLinecap="round"
          transform={`rotate(${hourAngle} 120 120)`}
        />
        <line
          x1="120"
          y1="120"
          x2="120"
          y2="40"
          stroke="var(--color-accent)"
          strokeWidth="3"
          strokeLinecap="round"
          transform={`rotate(${minuteAngle} 120 120)`}
        />
        <circle
          cx="120"
          cy="72"
          r="16"
          fill="var(--color-ivory)"
          transform={`rotate(${hourAngle} 120 120)`}
          onPointerDown={(event) => down("hour", event)}
          onPointerMove={(event) => {
            if (handRef.current === "hour") apply("hour", event);
          }}
          onPointerUp={() => {
            handRef.current = null;
            setHand(null);
          }}
        />
        <circle
          cx="120"
          cy="40"
          r="14"
          fill="var(--color-accent)"
          transform={`rotate(${minuteAngle} 120 120)`}
          onPointerDown={(event) => down("minute", event)}
          onPointerMove={(event) => {
            if (handRef.current === "minute") apply("minute", event);
          }}
          onPointerUp={() => {
            handRef.current = null;
            setHand(null);
          }}
        />
      </svg>
      <div className="flex gap-2">
        {(["AM", "PM"] as const).map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => {
              setMeridiem(value);
              publish(hour, minute, value);
              tick(8);
            }}
            className={`h-10 rounded-full px-4 text-sm font-semibold ${
              meridiem === value ? "bg-ivory text-night" : "bg-panel text-ivory"
            }`}
          >
            {value}
          </button>
        ))}
      </div>
    </div>
  );
}
