import type { LockKind } from "@/lib/vault/types";
import type { LockProps } from "@/components/locks/props";
import { LatticeLock } from "@/components/locks/LatticeLock";
import { CipherLock } from "@/components/locks/CipherLock";
import { WheelLock } from "@/components/locks/WheelLock";
import { ChromaLock } from "@/components/locks/ChromaLock";
import { GlyphLock } from "@/components/locks/GlyphLock";
import { ChronoLock } from "@/components/locks/ChronoLock";
import { SignalLock } from "@/components/locks/SignalLock";
import { FormulaLock } from "@/components/locks/FormulaLock";
import { OrbitLock } from "@/components/locks/OrbitLock";
import { VectorLock } from "@/components/locks/VectorLock";

export function LockView({ kind, ...props }: LockProps & { kind: LockKind }) {
  switch (kind) {
    case "lattice":
      return <LatticeLock {...props} />;
    case "cipher":
      return <CipherLock {...props} />;
    case "tumblers":
      return <WheelLock {...props} />;
    case "chroma":
      return <ChromaLock {...props} />;
    case "glyph":
      return <GlyphLock {...props} />;
    case "chrono":
      return <ChronoLock {...props} />;
    case "signal":
      return <SignalLock {...props} />;
    case "formula":
      return <FormulaLock {...props} />;
    case "orbit":
      return <OrbitLock {...props} />;
    case "vector":
      return <VectorLock {...props} />;
    default: {
      const neverKind: never = kind;
      return neverKind;
    }
  }
}
