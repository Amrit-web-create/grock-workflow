export type LockMode = "edit" | "challenge";

export interface LockProps {
  mode: LockMode;
  nonce: number;
  onCommit: (canonical: string) => void;
  onDraft: (canonical: string | null) => void;
  onReject: (message: string) => void;
}
