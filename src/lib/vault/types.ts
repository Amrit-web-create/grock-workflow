export const LOCK_KINDS = [
  "lattice",
  "cipher",
  "tumblers",
  "chroma",
  "glyph",
  "chrono",
  "signal",
  "formula",
  "orbit",
  "vector",
] as const;

export type LockKind = (typeof LOCK_KINDS)[number];

export type RelockMode = "immediate" | "screenoff" | "grace";

export interface Settings {
  armed: boolean;
  relock: RelockMode;
  graceSec: number;
  biometric: boolean;
  credentialId: string | null;
  intruderAfter: number;
  masterOn: boolean;
}

export interface Recovery {
  question: string;
  answerHash: string;
  pinHash: string;
}

export interface Assignment {
  appId: string;
  kind: LockKind;
  sealed: string;
  armed: boolean;
}

export interface MasterLock {
  kind: LockKind;
  sealed: string;
}

export interface IntruderEntry {
  id: string;
  at: number;
  appId: string;
  kind: LockKind;
  attempts: number;
  image: string | null;
  note: string | null;
}

export interface ActivityEntry {
  id: string;
  at: number;
  appId: string;
  kind: LockKind;
  ok: boolean;
}

export interface PersistedVault {
  version: 1;
  onboarded: boolean;
  recovery: Recovery | null;
  settings: Settings;
  master: MasterLock | null;
  assignments: Assignment[];
  intruders: IntruderEntry[];
  activity: ActivityEntry[];
}
