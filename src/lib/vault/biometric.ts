function bufToB64(buf: ArrayBuffer): string {
  const bytes = new Uint8Array(buf);
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function b64ToBuf(value: string): ArrayBuffer {
  const pad = value.replaceAll("-", "+").replaceAll("_", "/");
  const bin = atob(pad);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i += 1) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer;
}

export function biometricAvailable(): boolean {
  return typeof window !== "undefined" && typeof window.PublicKeyCredential === "function";
}

export async function enrollBiometric(): Promise<string> {
  if (!biometricAvailable()) {
    throw new Error("This device has no platform authenticator.");
  }
  const credential = (await navigator.credentials.create({
    publicKey: {
      challenge: crypto.getRandomValues(new Uint8Array(32)),
      rp: { name: "AppLock Studio" },
      user: {
        id: crypto.getRandomValues(new Uint8Array(16)),
        name: "owner",
        displayName: "Owner",
      },
      pubKeyCredParams: [
        { type: "public-key", alg: -7 },
        { type: "public-key", alg: -257 },
      ],
      authenticatorSelection: {
        authenticatorAttachment: "platform",
        userVerification: "required",
        residentKey: "preferred",
      },
      timeout: 60_000,
      attestation: "none",
    },
  })) as PublicKeyCredential | null;
  if (!credential) throw new Error("Enrollment was cancelled.");
  return bufToB64(credential.rawId);
}

export async function verifyBiometric(credentialId: string): Promise<void> {
  if (!biometricAvailable()) throw new Error("Biometrics are not available in this browser.");
  const result = await navigator.credentials.get({
    publicKey: {
      challenge: crypto.getRandomValues(new Uint8Array(32)),
      allowCredentials: [{ type: "public-key", id: b64ToBuf(credentialId) }],
      userVerification: "required",
      timeout: 60_000,
    },
  });
  if (!result) throw new Error("Biometric check was cancelled.");
}
