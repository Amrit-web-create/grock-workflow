import { useState } from "react";
import { matches, normalizeAnswer, seal } from "@/lib/vault/seal";
import { useVault } from "@/lib/vault/store";

const QUESTIONS = [
  "What city were you born in?",
  "What was the name of your first school?",
  "What was your childhood nickname?",
  "What was the first concert you attended?",
];

type Perm = "unknown" | "granted" | "denied" | "unavailable";

export function Onboarding() {
  const setOnboarded = useVault((state) => state.setOnboarded);
  const [step, setStep] = useState(0);
  const [notes, setNotes] = useState<Perm>("unknown");
  const [camera, setCamera] = useState<Perm>("unknown");
  const [question, setQuestion] = useState(QUESTIONS[0] ?? "");
  const [custom, setCustom] = useState("");
  const [answer, setAnswer] = useState("");
  const [pin, setPin] = useState("");
  const [again, setAgain] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function askNotes() {
    try {
      if (typeof Notification === "undefined") {
        setNotes("unavailable");
        return;
      }
      const result = await Notification.requestPermission();
      setNotes(result === "granted" ? "granted" : "denied");
    } catch {
      setNotes("denied");
    }
  }

  async function askCamera() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
      stream.getTracks().forEach((track) => track.stop());
      setCamera("granted");
    } catch {
      setCamera("denied");
    }
  }

  async function finish() {
    const chosen = custom.trim() || question;
    if (chosen.length < 8) {
      setError("Write a question you’ll still understand later.");
      return;
    }
    if (normalizeAnswer(answer).length < 2) {
      setError("The answer is too short.");
      return;
    }
    if (!/^\d{6}$/.test(pin)) {
      setError("Recovery PIN must be 6 digits.");
      return;
    }
    if (pin !== again) {
      setError("The PIN and the confirmation don’t match.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const answerHash = await seal(normalizeAnswer(answer));
      const pinHash = await seal(pin);
      const ok = (await matches(normalizeAnswer(answer), answerHash)) && (await matches(pin, pinHash));
      if (!ok) throw new Error("Could not verify the recovery seal.");
      setOnboarded({ question: chosen, answerHash, pinHash });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save recovery.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-lg flex-col justify-center px-5 py-10">
      <p className="text-xs font-semibold tracking-widest text-muted">APPLOCK STUDIO</p>
      <h1 className="mt-2 font-display text-4xl leading-tight text-ink">
        {step === 0 ? "A private lock for each app." : step === 1 ? "Permissions, only if you want them." : "A way back in."}
      </h1>
      <p className="mt-3 text-muted">
        {step === 0
          ? "Ten different locks. Assign any of them per app. Nothing here is uploaded, and there are no ads."
          : step === 1
            ? "The studio works without these. Camera is used only after repeated wrong attempts. Notifications keep a phone service alive in the Android build."
            : "If a lock fails you, the answer and a 6-digit PIN open it. They stay hashed on this device."}
      </p>

      {step === 1 ? (
        <div className="mt-8 flex flex-col gap-3">
          <PermRow label="Notifications" status={notes} onAsk={askNotes} />
          <PermRow label="Camera" status={camera} onAsk={askCamera} />
          <div className="rounded-2xl border border-line bg-surface px-4 py-3">
            <div className="flex items-center justify-between gap-3">
              <span className="font-medium">Lock overlay</span>
              <span className="text-sm text-accent">Ready</span>
            </div>
            <p className="mt-1 text-sm text-muted">
              In this studio the lock covers the page. The Android project draws it over other apps.
            </p>
          </div>
        </div>
      ) : null}

      {step === 2 ? (
        <div className="mt-8 flex flex-col gap-3">
          <label className="text-sm font-medium" htmlFor="question">
            Security question
          </label>
          <select
            id="question"
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            className="h-12 rounded-2xl border border-line bg-surface px-3"
          >
            {QUESTIONS.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
          <input
            value={custom}
            onChange={(event) => setCustom(event.target.value)}
            placeholder="Or write your own question"
            className="h-12 rounded-2xl border border-line bg-surface px-3"
          />
          <input
            value={answer}
            onChange={(event) => setAnswer(event.target.value)}
            placeholder="Answer"
            autoComplete="off"
            className="h-12 rounded-2xl border border-line bg-surface px-3"
          />
          <input
            value={pin}
            onChange={(event) => setPin(event.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="6-digit recovery PIN"
            inputMode="numeric"
            autoComplete="off"
            className="h-12 rounded-2xl border border-line bg-surface px-3 tracking-widest"
          />
          <input
            value={again}
            onChange={(event) => setAgain(event.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="Confirm PIN"
            inputMode="numeric"
            autoComplete="off"
            className="h-12 rounded-2xl border border-line bg-surface px-3 tracking-widest"
          />
        </div>
      ) : null}

      {error ? (
        <p className="mt-4 text-sm text-danger" role="status">
          {error}
        </p>
      ) : null}

      <div className="mt-8 flex gap-3">
        {step > 0 ? (
          <button
            type="button"
            onClick={() => {
              setError(null);
              setStep((value) => value - 1);
            }}
            className="h-12 rounded-full px-5 font-semibold text-ink"
          >
            Back
          </button>
        ) : null}
        {step < 2 ? (
          <button
            type="button"
            onClick={() => setStep((value) => value + 1)}
            className="h-12 rounded-full bg-accent px-6 font-semibold text-accent-ink"
          >
            Continue
          </button>
        ) : (
          <button
            type="button"
            disabled={busy}
            onClick={() => void finish()}
            className="h-12 rounded-full bg-accent px-6 font-semibold text-accent-ink disabled:opacity-50"
          >
            {busy ? "Saving" : "Enter studio"}
          </button>
        )}
      </div>
    </main>
  );
}

function PermRow({ label, status, onAsk }: { label: string; status: Perm; onAsk: () => void }) {
  const text =
    status === "granted" ? "Allowed" : status === "denied" ? "Blocked" : status === "unavailable" ? "Unavailable" : "Not requested";
  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl border border-line bg-surface px-4 py-3">
      <div>
        <p className="font-medium">{label}</p>
        <p className="text-sm text-muted">{text}</p>
      </div>
      {status === "granted" ? null : (
        <button type="button" onClick={onAsk} className="h-10 rounded-full bg-canvas px-4 text-sm font-semibold">
          Allow
        </button>
      )}
    </div>
  );
}
