function hex(bytes: Uint8Array): string {
  let out = "";
  for (const b of bytes) out += b.toString(16).padStart(2, "0");
  return out;
}

export async function sha256(text: string): Promise<string> {
  const data = new TextEncoder().encode(text);
  const buf = await crypto.subtle.digest("SHA-256", data);
  return hex(new Uint8Array(buf));
}

export async function seal(canonical: string): Promise<string> {
  const saltBytes = crypto.getRandomValues(new Uint8Array(16));
  const salt = hex(saltBytes);
  const hash = await sha256(`${salt}:${canonical}`);
  return `${salt}:${hash}`;
}

export async function matches(canonical: string, sealed: string): Promise<boolean> {
  const split = sealed.indexOf(":");
  if (split < 1) return false;
  const salt = sealed.slice(0, split);
  const hash = sealed.slice(split + 1);
  const next = await sha256(`${salt}:${canonical}`);
  if (next.length !== hash.length) return false;
  let diff = 0;
  for (let i = 0; i < next.length; i += 1) {
    diff |= next.charCodeAt(i) ^ hash.charCodeAt(i);
  }
  return diff === 0;
}

export function normalizeAnswer(value: string): string {
  return value.trim().toLowerCase().replace(/\s+/g, " ");
}
