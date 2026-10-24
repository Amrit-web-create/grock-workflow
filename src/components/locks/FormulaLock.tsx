import { useEffect, useState } from "react";
import { Delete } from "lucide-react";
import { normalizeExpr } from "@/lib/vault/expr";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const KEYS = ["7", "8", "9", "÷", "4", "5", "6", "×", "1", "2", "3", "−", "(", "0", ")", "+"] as const;

export function FormulaLock({ nonce, onCommit, onReject }: LockProps) {
  const [expr, setExpr] = useState("");

  useEffect(() => {
    setExpr("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function push(key: string) {
    setExpr((current) => (current + key).slice(0, 24));
    tick(6);
  }

  function submit() {
    const canonical = normalizeExpr(expr);
    if (!canonical) {
      onReject("Use digits and at least one operation. Equals checks it.");
      return;
    }
    onCommit(canonical);
  }

  return (
    <div className="lock-ui flex w-full max-w-xs flex-col gap-3">
      <div className="flex h-14 items-center justify-end rounded-2xl bg-panel px-4">
        <span className="truncate font-display text-2xl text-ivory">{expr || "0"}</span>
      </div>
      <div className="grid grid-cols-4 gap-2">
        {KEYS.map((key) => (
          <button
            key={key}
            type="button"
            onClick={() => push(key)}
            className="flex h-12 items-center justify-center rounded-xl bg-panel font-display text-xl text-ivory"
          >
            {key}
          </button>
        ))}
        <button
          type="button"
          aria-label="Clear"
          onClick={() => setExpr("")}
          className="h-12 rounded-xl bg-panel text-sm font-semibold text-mist"
        >
          Clear
        </button>
        <button
          type="button"
          aria-label="Backspace"
          onClick={() => setExpr((current) => current.slice(0, -1))}
          className="flex h-12 items-center justify-center rounded-xl bg-panel text-ivory"
        >
          <Delete size={18} />
        </button>
        <button
          type="button"
          onClick={submit}
          className="col-span-2 h-12 rounded-xl bg-ivory font-semibold text-night"
        >
          Equals
        </button>
      </div>
    </div>
  );
}
