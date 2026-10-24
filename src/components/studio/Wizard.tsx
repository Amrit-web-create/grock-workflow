import { useRef, useState } from "react";
import { LockView } from "@/components/locks/LockView";
import { CATALOG } from "@/lib/vault/catalog";
import { lockMeta, weakness } from "@/lib/vault/locks";
import { seal } from "@/lib/vault/seal";
import { useVault } from "@/lib/vault/store";
import { grantFor } from "@/lib/vault/grants";
import { tick } from "@/lib/vault/haptic";
import type { LockKind } from "@/lib/vault/types";
import { AppMark } from "@/components/studio/marks";

const STEPS = ["Draw", "Confirm", "Practice", "Arm"] as const;
type Step = "edit" | "confirm" | "practice" | "assign";

export function Wizard({
  kind,
  initialApps,
  master,
  onClose,
  onSaved,
}: {
  kind: LockKind;
  initialApps: string[];
  master: boolean;
  onClose: () => void;
  onSaved: (text: string) => void;
}) {
  const meta = lockMeta(kind);
  const [step, setStep] = useState<Step>("edit");
  const [first, setFirst] = useState<string | null>(null);
  const [live, setLive] = useState<string | null>(null);
  const [nonce, setNonce] = useState(1);
  const [streak, setStreak] = useState(0);
  const [message, setMessage] = useState<string | null>(null);
  const [shake, setShake] = useState(0);
  const [allowWeak, setAllowWeak] = useState(false);
  const [busy, setBusy] = useState(false);
  const [picked, setPicked] = useState<string[]>(initialApps);
  const liveRef = useRef<string | null>(null);

  function remember(next: string | null) {
    if (liveRef.current !== next) setAllowWeak(false);
    liveRef.current = next;
    setLive(next);
  }

  function resetBoard() {
    liveRef.current = null;
    setLive(null);
    setNonce((value) => value + 1);
  }

  function fail(text: string) {
    setMessage(text);
    setShake((value) => value + 1);
    if (step === "practice") setStreak(0);
    resetBoard();
    tick(18);
  }

  function challenge(canonical: string) {
    if (!first || canonical !== first) {
      fail(meta.miss);
      return;
    }
    tick(14);
    if (step === "confirm") {
      setMessage("Matched. Now open it three times without missing.");
      setStreak(0);
      setStep("practice");
      resetBoard();
      return;
    }
    const next = streak + 1;
    setStreak(next);
    if (next >= 3) {
      setMessage(null);
      setStep("assign");
      resetBoard();
      return;
    }
    setMessage(`${next} of 3. Again.`);
    resetBoard();
  }

  function onCommit(canonical: string) {
    if (step === "edit") {
      if (meta.submit === "gesture") remember(canonical);
      return;
    }
    if (meta.submit === "gesture") challenge(canonical);
  }

  function onDraft(canonical: string | null) {
    if (meta.submit === "confirm" || canonical === null) remember(canonical);
  }

  function press() {
    if (!live) {
      setMessage(meta.need);
      setShake((value) => value + 1);
      return;
    }
    if (step === "edit") {
      const warn = weakness(kind, live);
      if (warn && !allowWeak) {
        setAllowWeak(true);
        setMessage(warn);
        return;
      }
      setFirst(live);
      setMessage("Enter the same secret again.");
      setStep("confirm");
      setAllowWeak(false);
      resetBoard();
      return;
    }
    challenge(live);
  }

  async function arm() {
    if (!first) return;
    if (!master && picked.length === 0) {
      setMessage("Choose at least one app.");
      return;
    }
    setBusy(true);
    try {
      if (master) {
        const sealed = await seal(first);
        useVault.getState().setMaster({ kind, sealed });
        grantFor("master", 8 * 60 * 60 * 1000);
        onSaved("Studio lock is on.");
      } else {
        for (const appId of picked) {
          const sealed = await seal(first);
          useVault.getState().upsertAssignment({ appId, kind, sealed, armed: true });
        }
        const names = picked
          .map((id) => CATALOG.find((app) => app.id === id)?.name ?? id)
          .join(", ");
        onSaved(`${meta.name} now protects ${names}.`);
      }
      onClose();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not save this lock.");
    } finally {
      setBusy(false);
    }
  }

  const warn = step === "edit" && live ? weakness(kind, live) : null;
  const showCheck = step !== "assign" && (meta.submit === "confirm" || step === "edit");
  const stepIndex = step === "edit" ? 0 : step === "confirm" ? 1 : step === "practice" ? 2 : 3;

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-lg flex-col px-4 py-5">
      <div className="flex items-center justify-between">
        <button type="button" onClick={onClose} className="text-sm font-semibold text-muted">
          Close
        </button>
        <p className="text-sm font-semibold">{meta.name}</p>
      </div>
      <ol className="mt-4 grid grid-cols-4 gap-2 text-xs">
        {STEPS.map((label, index) => (
          <li key={label} className={index <= stepIndex ? "text-ink" : "text-muted"}>
            <span className={`mb-1 block h-1 rounded-full ${index <= stepIndex ? "bg-accent" : "bg-line"}`} />
            {label}
          </li>
        ))}
      </ol>

      {step === "assign" ? (
        <section className="mt-6">
          <h1 className="font-display text-3xl">Arm it</h1>
          <p className="mt-2 text-muted">
            {master
              ? "This lock will sit in front of the studio. Recovery still works if you forget it."
              : "The same secret is sealed separately for each app you pick."}
          </p>
          {master ? null : (
            <ul className="mt-5 flex max-h-96 flex-col gap-2 overflow-auto">
              {CATALOG.map((app) => {
                const on = picked.includes(app.id);
                return (
                  <li key={app.id}>
                    <button
                      type="button"
                      onClick={() =>
                        setPicked((current) =>
                          current.includes(app.id)
                            ? current.filter((id) => id !== app.id)
                            : [...current, app.id],
                        )
                      }
                      className={`flex w-full items-center gap-3 rounded-2xl border px-3 py-2 text-left ${
                        on ? "border-accent bg-surface" : "border-line bg-surface"
                      }`}
                    >
                      <AppMark id={app.id} />
                      <span className="flex-1">
                        <span className="block font-medium">{app.name}</span>
                        <span className="text-sm text-muted">{app.blurb}</span>
                      </span>
                      <span className="text-sm font-semibold text-accent">{on ? "On" : "Off"}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
          {message ? (
            <p className="mt-3 text-sm text-danger" role="status">
              {message}
            </p>
          ) : null}
          <button
            type="button"
            disabled={busy}
            onClick={() => void arm()}
            className="mt-5 h-12 rounded-full bg-accent px-6 font-semibold text-accent-ink disabled:opacity-50"
          >
            {busy ? "Sealing" : master ? "Arm studio lock" : "Arm selected apps"}
          </button>
        </section>
      ) : (
        <>
          <p className="mt-5 text-sm text-muted">{step === "edit" ? meta.summary : meta.kicker}</p>
          {step === "practice" ? (
            <p className="mt-1 text-sm font-medium">Practice {streak} of 3</p>
          ) : null}
          <div className="mt-4 rounded-3xl bg-night px-3 py-6 text-ivory">
            <div key={shake} className={shake > 0 ? "shake" : undefined}>
              <LockView
                key={nonce}
                kind={kind}
                mode={step === "edit" ? "edit" : "challenge"}
                nonce={nonce}
                onCommit={onCommit}
                onDraft={onDraft}
                onReject={(text) => setMessage(text)}
              />
            </div>
          </div>
          <p className="mt-3 min-h-6 text-sm text-muted" role="status">
            {message || (warn && allowWeak ? warn : "")}
          </p>
          {showCheck ? (
            <button
              type="button"
              onClick={press}
              className="h-12 rounded-full bg-accent px-6 font-semibold text-accent-ink"
            >
              {step === "edit" && warn && allowWeak ? "Keep it anyway" : step === "edit" ? "Continue" : "Check"}
            </button>
          ) : (
            <p className="text-sm text-muted">Finish the gesture to continue.</p>
          )}
        </>
      )}
    </main>
  );
}
