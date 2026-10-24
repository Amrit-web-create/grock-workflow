import { useEffect, useState } from "react";
import { tick } from "@/lib/vault/haptic";
import type { LockProps } from "@/components/locks/props";

const SOLVED = [1, 2, 3, 4, 5, 6, 7, 8, 0];

function neighbors(index: number): number[] {
  const row = Math.floor(index / 3);
  const col = index % 3;
  const next: number[] = [];
  if (row > 0) next.push(index - 3);
  if (row < 2) next.push(index + 3);
  if (col > 0) next.push(index - 1);
  if (col < 2) next.push(index + 1);
  return next;
}

function keyOf(board: number[]): string {
  return board.join("");
}

export function LatticeLock({ nonce, onDraft, onReject }: LockProps) {
  const [board, setBoard] = useState<number[]>(SOLVED);
  const [moves, setMoves] = useState(0);

  useEffect(() => {
    setBoard(SOLVED);
    setMoves(0);
    onDraft(null);
    // Parent callback is stable.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nonce]);

  function commitBoard(next: number[], nextMoves: number) {
    const key = keyOf(next);
    if (nextMoves >= 5 && key !== keyOf(SOLVED)) onDraft(key);
    else onDraft(null);
  }

  function trySlide(index: number) {
    const blank = board.indexOf(0);
    if (!neighbors(blank).includes(index)) {
      onReject("Slide a tile that touches the empty cell.");
      return;
    }
    const next = board.slice();
    const blankIndex = blank;
    next[blankIndex] = next[index] ?? 0;
    next[index] = 0;
    const nextMoves = moves + 1;
    setBoard(next);
    setMoves(nextMoves);
    commitBoard(next, nextMoves);
    tick(8);
  }

  return (
    <div className="lock-ui flex flex-col items-center gap-4">
      <p className="text-sm text-mist">
        {moves === 0 ? "Start from solved. Scramble with intention." : `${moves} moves`}
      </p>
      <div className="relative h-64 w-64 rounded-3xl bg-night">
        {board.map((n, index) => {
          if (n === 0) return null;
          const x = (index % 3) * 76 + 12;
          const y = Math.floor(index / 3) * 76 + 12;
          return (
            <button
              key={n}
              type="button"
              aria-label={`Tile ${n}`}
              onClick={() => trySlide(index)}
              className="tile-move absolute flex size-16 items-center justify-center rounded-2xl bg-ivory text-night"
              style={{ transform: `translate(${x}px, ${y}px)` }}
            >
              <span className="font-display text-3xl leading-none">{n}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
