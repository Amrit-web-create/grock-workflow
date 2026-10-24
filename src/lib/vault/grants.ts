const grants = new Map<string, number>();

export function hasGrant(id: string): boolean {
  const exp = grants.get(id);
  if (!exp) return false;
  if (exp > Date.now()) return true;
  grants.delete(id);
  return false;
}

export function grantFor(id: string, ms: number) {
  if (ms <= 0) {
    grants.delete(id);
    return;
  }
  grants.set(id, Date.now() + ms);
}

export function clearGrants() {
  grants.clear();
}

export function grantDuration(relock: "immediate" | "screenoff" | "grace", graceSec: number, master: boolean): number {
  if (master && relock === "immediate") return 8 * 60 * 60 * 1000;
  if (relock === "immediate") return 0;
  if (relock === "screenoff") return 12 * 60 * 60 * 1000;
  return graceSec * 1000;
}
