import { useRef, useState } from "react";
import { LockView } from "@/components/locks/LockView";
import { verifyBiometric } from "@/lib/vault/biometric";
import { captureIntruder } from "@/lib/vault/capture";
import { clearGrants } from "@/lib/vault/grants";
import { tick } from "@/lib/vault/haptic";
import { lockMeta } from "@/lib/vault/locks";
import { matches, normalizeAnswer } from "@/lib/vault/seal";
import { newId, useVault } from "@/lib/vault/store";
import type { LockKind } from "@/lib/vault/types";

export function LockOverlay({
  title,
  detail,
  kind,
  sealed,
  appId,
  cancellable,
  onUnlocked,
  onCancel,
}: {
  title: string;
  detail: string;
  kind: LockKind;
  sealed: string;
  appId: string;
  cancellable: boolean;
  onUnlocked: () => void;
  onCancel: () => void;
}) {
  const meta = lockMeta(kind);
  const settings = useVault((state) => state.settings);
  const recovery = useVault((state) => state.recovery);
  const [nonce, setNonce] = useState(1);
  const [shake, setShake] = useState(0);
  const [message, setMessage] = useState<string | null>(null);
  const [fails, setFails] = useState(0);
  const [phase, setPhase] = useState<"lock" | "ok" | "recover">("lock");
  const [busy, setBusy] = useState(false);
  const [answer, setAnswer] = useState("");
  const [pin, setPin] = useState("");
  const [holding, setHolding] = useState(false);
  const failsRef = useRef(0);
  const eraseTimer = useRef<number | null>(null);
  const logged = useRef(false);

  async function unlock() {
    setPhase("ok");
    tick(16);
    window.setTimeout(onUnlocked, 420);
  }

  async function miss() {
    const next = failsRef.current + 1;
    failsRef.current = next;
    setFails(next);
    setMessage(meta.miss);
    setShake((value) => value + 1);
    setNonce((value) => value + 1);
    tick(20);
    useVault.getState().addActivity({
      id: newId(),
      at: Date.now(),
      appId,
      kind,
      ok: false,
    });
    if (next % Math.max(1, settings.intruderAfter) === 0) {
      const shot = await captureIntruder();
      useVault.getState().addIntruder({
        id: newId(),
        at: Date.now(),
        appId,
        kind,
        attempts: next,
        image: shot.image,
        note: shot.note,
      });
      setMessage(shot.note ?? "Attempt saved to alerts.");
    }
  }

  async function onCommit(canonical: string) {
    if (busy || phase !== "lock") return;
    if (meta.submit === "confirm") return;
    setBusy(true);
    try {
      const ok = await matches(canonical, sealed);
      if (ok) await unlock();
      else await miss();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not check that attempt.");
    } finally {
      setBusy(false);
    }
  }

  const live = useRef<string | null>(null);

  async function check() {
    if (!live.current) {
      setMessage(meta.need);
      setShake((value) => value + 1);
      return;
    }
    setBusy(true);
    try {
      const ok = await matches(live.current, sealed);
      if (ok) await unlock();
      else await miss();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not check that attempt.");
    } finally {
      setBusy(false);
    }
  }

  async function biometric() {
    if (!settings.credentialId) {
      setMessage("Enroll biometrics in Settings first.");
      return;
    }
    setBusy(true);
    try {
      await verifyBiometric(settings.credentialId);
      await unlock();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Biometric check failed.");
    } finally {
      setBusy(false);
    }
  }

  async function recover(event: React.FormEvent) {
    event.preventDefault();
    if (!recovery) {
      setMessage("Recovery was not set up.");
      return;
    }
    if (logged.current) return;
    setBusy(true);
    try {
      const answerOk = await matches(normalizeAnswer(answer), recovery.answerHash);
      const pinOk = await matches(pin, recovery.pinHash);
      if (!answerOk || !pinOk) {
        setMessage("Recovery doesn’t match.");
        setShake((value) => value + 1);
        return;
      }
      logged.current = true;
      await unlock();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not check recovery.");
    } finally {
      setBusy(false);
    }
  }

  function startErase() {
    setHolding(true);
    eraseTimer.current = window.setTimeout(() => {
      clearGrants();
      useVault.getState().resetAll();
    }, 1400);
  }

  function stopErase() {
    setHolding(false);
    if (eraseTimer.current) window.clearTimeout(eraseTimer.current);
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-night text-ivory">
      <header className="flex items-start justify-between gap-4 px-5 pt-5">
        <div>
          <p className="text-xs font-semibold tracking-widest text-mist">{meta.kicker}</p>
          <h1 className="font-display text-3xl leading-tight">{title}</h1>
          <p className="mt-1 text-sm text-mist">{detail}</p>
        </div>
        {cancellable ? (
          <button type="button" onClick={onCancel} className="text-sm font-semibold text-mist">
            Close
          </button>
        ) : null}
      </header>

      {phase === "ok" ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-3">
          <svg viewBox="0 0 80 80" className="size-20" aria-hidden="true">
            <circle cx="40" cy="40" r="28" fill="none" stroke="var(--color-ivory)" strokeWidth="2" className="seal-ring" />
            <path d="M28 41 l8 8 16-18" fill="none" stroke="var(--color-ivory)" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
          <p className="font-display text-2xl">Unlocked</p>
        </div>
      ) : phase === "recover" ? (
        <form onSubmit={(event) => void recover(event)} className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-3 px-5">
          <p className="text-sm text-mist">{recovery?.question}</p>
          <input
            value={answer}
            onChange={(event) => setAnswer(event.target.value)}
            placeholder="Answer"
            autoComplete="off"
            className="h-12 rounded-2xl bg-panel px-3 text-ivory"
          />
          <input
            value={pin}
            onChange={(event) => setPin(event.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="Recovery PIN"
            inputMode="numeric"
            autoComplete="off"
            className="h-12 rounded-2xl bg-panel px-3 tracking-widest text-ivory"
          />
          {message ? (
            <p className="text-sm text-ivory" role="status">
              {message}
            </p>
          ) : null}
          <button type="submit" disabled={busy} className="h-12 rounded-full bg-ivory font-semibold text-night">
            Open with recovery
          </button>
          <button type="button" onClick={() => setPhase("lock")} className="h-11 text-sm text-mist">
            Back to lock
          </button>
        </form>
      ) : (
        <>
          <div className="flex-1 overflow-y-auto">
            <div className="flex min-h-full flex-col items-center justify-center px-4 py-6">
              <div key={shake} className={shake > 0 ? "shake" : undefined}>
                <LockView
                  key={nonce}
                  kind={kind}
                  mode="challenge"
                  nonce={nonce}
                  onCommit={(canonical) => void onCommit(canonical)}
                  onDraft={(canonical) => {
                    live.current = canonical;
                  }}
                  onReject={(text) => setMessage(text)}
                />
              </div>
              <p className="mt-4 min-h-6 text-center text-sm text-mist" role="status">
                {message}
              </p>
              {meta.submit === "confirm" ? (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void check()}
                  className="h-12 rounded-full bg-ivory px-6 font-semibold text-night disabled:opacity-50"
                >
                  Check
                </button>
              ) : null}
            </div>
          </div>
          <footer className="flex flex-col items-center gap-1 px-5 pb-6">
            <p className="text-xs tracking-widest text-mist">
              {fails === 0 ? "No missed attempts" : `${fails} missed`}
            </p>
            {settings.biometric ? (
              <button type="button" onClick={() => void biometric()} className="h-11 text-sm font-semibold text-ivory">
                Use device biometrics
              </button>
            ) : null}
            <button type="button" onClick={() => setPhase("recover")} className="h-11 text-sm text-mist">
              Forgot this lock
            </button>
            <button
              type="button"
              onPointerDown={startErase}
              onPointerUp={stopErase}
              onPointerLeave={stopErase}
              className="h-11 text-xs text-mist"
            >
              {holding ? "Keep holding to erase this studio" : "Hold to erase everything"}
            </button>
          </footer>
        </>
      )}
    </div>
  );
}
