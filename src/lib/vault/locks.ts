import type { LockKind } from "@/lib/vault/types";

export interface LockMeta {
  id: LockKind;
  name: string;
  kicker: string;
  summary: string;
  miss: string;
  need: string;
  submit: "gesture" | "confirm";
  hardness: string;
}

export const LOCKS: LockMeta[] = [
  {
    id: "lattice",
    name: "Lattice",
    kicker: "Sliding seal",
    summary: "Slide the tiles into an arrangement only you know. At least five moves from solved.",
    miss: "That arrangement doesn’t match.",
    need: "Make at least five moves, and don’t leave it solved.",
    submit: "confirm",
    hardness: "Very high",
  },
  {
    id: "cipher",
    name: "Cipher",
    kicker: "Shifting numerals",
    summary: "Hold four numerals while the grid keeps shifting. Positions are a distraction.",
    miss: "That numeral sequence doesn’t match.",
    need: "Hold each cell until the ring closes. Four numerals.",
    submit: "gesture",
    hardness: "High",
  },
  {
    id: "tumblers",
    name: "Tumblers",
    kicker: "Vault wheels",
    summary: "Three weighted wheels. Let them settle, then check the combination.",
    miss: "That combination doesn’t match.",
    need: "Settle all three wheels.",
    submit: "confirm",
    hardness: "High",
  },
  {
    id: "chroma",
    name: "Chroma",
    kicker: "Color order",
    summary: "Five tones, in order. Repeats are allowed. Nothing is shown back to you later.",
    miss: "That color order doesn’t match.",
    need: "Tap five tones.",
    submit: "gesture",
    hardness: "High",
  },
  {
    id: "glyph",
    name: "Glyph",
    kicker: "Shape docks",
    summary: "Seat four shapes in order, then tap the corner that seals them.",
    miss: "Those shapes don’t match.",
    need: "Fill every dock, then choose a corner.",
    submit: "gesture",
    hardness: "High",
  },
  {
    id: "chrono",
    name: "Chrono",
    kicker: "Clock hands",
    summary: "A private time. The hour and minute hands move independently.",
    miss: "That time doesn’t match.",
    need: "Set both hands.",
    submit: "confirm",
    hardness: "Medium",
  },
  {
    id: "signal",
    name: "Signal",
    kicker: "Symbol matrix",
    summary: "Four symbols. The grid shuffles on every attempt, so remember the marks, not the places.",
    miss: "That symbol order doesn’t match.",
    need: "Tap four different symbols.",
    submit: "gesture",
    hardness: "Very high",
  },
  {
    id: "formula",
    name: "Formula",
    kicker: "Expression",
    summary: "Not a PIN. Reproduce the same expression, including the operations.",
    miss: "That expression doesn’t match.",
    need: "Enter an expression with at least one operation, then press equals.",
    submit: "gesture",
    hardness: "High",
  },
  {
    id: "orbit",
    name: "Orbit",
    kicker: "Drifting numerals",
    summary: "Five drifting numerals, in an order you choose. They never sit still.",
    miss: "That order doesn’t match.",
    need: "Tap five different numerals.",
    submit: "gesture",
    hardness: "High",
  },
  {
    id: "vector",
    name: "Vector",
    kicker: "Direction path",
    summary: "Five straight swipes. Diagonal gestures are ignored on purpose.",
    miss: "That path doesn’t match.",
    need: "Swipe five clear directions.",
    submit: "gesture",
    hardness: "High",
  },
];

export function lockMeta(id: LockKind): LockMeta {
  const found = LOCKS.find((lock) => lock.id === id);
  if (!found) throw new Error(`Unknown lock ${id}`);
  return found;
}

export function weakness(kind: LockKind, canonical: string): string | null {
  if (kind === "tumblers") {
    if (/^(\d)\1\1$/.test(canonical) || ["012", "123", "321", "000", "111", "258"].includes(canonical)) {
      return "This combination is easy to guess.";
    }
  }
  if (kind === "chrono" && /:00:/.test(canonical)) {
    return "An exact hour is easy to guess.";
  }
  if (kind === "cipher") {
    if (/^(\d)\1\1\1$/.test(canonical) || ["0123", "1234", "4321", "0000", "1111"].includes(canonical)) {
      return "This sequence is easy to guess.";
    }
  }
  if (kind === "chroma") {
    const parts = canonical.split(".");
    if (parts.length > 0 && parts.every((part) => part === parts[0])) {
      return "A single repeated tone is easy to guess.";
    }
  }
  if (kind === "vector") {
    const parts = canonical.split("-");
    if (parts.length > 0 && parts.every((part) => part === parts[0])) {
      return "Five swipes in one direction are easy to guess.";
    }
  }
  if (kind === "lattice" && canonical.length === 9) {
    let dist = 0;
    for (let i = 0; i < 9; i += 1) {
      const n = Number(canonical[i]);
      if (n === 0) continue;
      const target = n - 1;
      dist += Math.abs((i % 3) - (target % 3)) + Math.abs(Math.floor(i / 3) - Math.floor(target / 3));
    }
    if (dist < 4) return "This arrangement sits too close to solved.";
  }
  return null;
}
