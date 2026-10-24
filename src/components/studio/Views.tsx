import { useMemo, useState } from "react";
import * as Switch from "@radix-ui/react-switch";
import { KeyRound, Shield } from "lucide-react";
import { appById, CATALOG } from "@/lib/vault/catalog";
import { biometricAvailable, enrollBiometric } from "@/lib/vault/biometric";
import { kindInUse, useVault } from "@/lib/vault/store";
import { LOCKS, lockMeta } from "@/lib/vault/locks";
import { matches, normalizeAnswer, seal } from "@/lib/vault/seal";
import type { LockKind, RelockMode } from "@/lib/vault/types";
import { AppMark, formatWhen } from "@/components/studio/marks";

function Toggle({
  checked,
  label,
  onChange,
}: {
  checked: boolean;
  label: string;
  onChange: (value: boolean) => void;
}) {
  return (
    <Switch.Root
      checked={checked}
      onCheckedChange={onChange}
      aria-label={label}
      className={`relative h-7 w-12 rounded-full ${checked ? "bg-accent" : "bg-line"}`}
    >
      <Switch.Thumb
        className={`block size-5 rounded-full bg-surface transition-transform ${checked ? "translate-x-6" : "translate-x-1"}`}
      />
    </Switch.Root>
  );
}

export function VaultView({
  onOpen,
  onBrowse,
}: {
  onOpen: (appId: string) => void;
  onBrowse: () => void;
}) {
  const settings = useVault((state) => state.settings);
  const assignments = useVault((state) => state.assignments);
  const activity = useVault((state) => state.activity);
  const intruders = useVault((state) => state.intruders);
  const master = useVault((state) => state.master);
  const armedApps = assignments.filter((item) => item.armed);
  const relock =
    settings.relock === "immediate"
      ? "Immediately"
      : settings.relock === "screenoff"
        ? "After screen off"
        : `After ${settings.graceSec}s`;

  return (
    <div className="rise">
      <p className="text-xs font-semibold tracking-widest text-muted">VAULT</p>
      <h1 className="mt-1 font-display text-4xl text-ink">
        {settings.armed ? "Protection is on" : "Protection is off"}
      </h1>
      <p className="mt-2 max-w-xl text-muted">
        {master && settings.masterOn
          ? `Studio lock uses ${lockMeta(master.kind).name}. Each app can still use a different design.`
          : "The studio itself is open. App locks still apply when protection is on."}
      </p>
      <dl className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-4">
        {[
          ["Protected", String(armedApps.length)],
          ["Designs", String(new Set(armedApps.map((item) => item.kind)).size)],
          ["Alerts", String(intruders.length)],
          ["Relock", relock],
        ].map(([label, value]) => (
          <div key={label} className="rounded-2xl border border-line bg-surface px-4 py-3">
            <dt className="text-sm text-muted">{label}</dt>
            <dd className="mt-1 font-display text-2xl">{value}</dd>
          </div>
        ))}
      </dl>
      <section className="mt-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-semibold">Open a protected app</h2>
          <button type="button" onClick={onBrowse} className="text-sm font-semibold text-accent">
            All apps
          </button>
        </div>
        {armedApps.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-line bg-surface px-4 py-8 text-center">
            <p className="font-medium">Nothing is protected yet</p>
            <p className="mt-1 text-sm text-muted">Pick a lock, practice it three times, then arm an app.</p>
            <button
              type="button"
              onClick={onBrowse}
              className="mt-4 h-11 rounded-full bg-accent px-5 text-sm font-semibold text-accent-ink"
            >
              Choose an app
            </button>
          </div>
        ) : (
          <ul className="grid gap-2 sm:grid-cols-2">
            {armedApps.map((item) => {
              const app = appById(item.appId);
              if (!app) return null;
              return (
                <li key={item.appId}>
                  <button
                    type="button"
                    onClick={() => onOpen(item.appId)}
                    className="flex w-full items-center gap-3 rounded-2xl border border-line bg-surface px-3 py-3 text-left"
                  >
                    <AppMark id={app.id} />
                    <span>
                      <span className="block font-medium">{app.name}</span>
                      <span className="text-sm text-muted">{lockMeta(item.kind).name}</span>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </section>
      <section className="mt-8">
        <h2 className="font-semibold">Recent</h2>
        {activity.length === 0 ? (
          <p className="mt-2 text-sm text-muted">Unlocks and misses will show up here.</p>
        ) : (
          <ul className="mt-3 divide-y divide-line rounded-2xl border border-line bg-surface">
            {activity.slice(0, 8).map((item) => (
              <li key={item.id} className="flex items-center justify-between gap-3 px-4 py-3 text-sm">
                <span>
                  {appById(item.appId)?.name ?? "Studio"} · {lockMeta(item.kind).name}
                </span>
                <span className={item.ok ? "text-muted" : "text-danger"}>
                  {item.ok ? "Opened" : "Missed"} · {formatWhen(item.at)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

export function AppsView({
  onOpen,
  onAssign,
}: {
  onOpen: (appId: string) => void;
  onAssign: (appId: string) => void;
}) {
  const assignments = useVault((state) => state.assignments);
  const setArmed = useVault((state) => state.setAssignmentArmed);
  const clearAssignment = useVault((state) => state.clearAssignment);
  const [query, setQuery] = useState("");
  const [group, setGroup] = useState<string>("All");
  const groups = ["All", ...new Set(CATALOG.map((app) => app.group))];
  const visible = useMemo(
    () =>
      CATALOG.filter((app) => {
        const blob = `${app.name} ${app.blurb}`.toLowerCase();
        const matchesQuery = blob.includes(query.trim().toLowerCase());
        return matchesQuery && (group === "All" || app.group === group);
      }),
    [group, query],
  );

  return (
    <div className="rise">
      <p className="text-xs font-semibold tracking-widest text-muted">APPS</p>
      <h1 className="mt-1 font-display text-4xl">Lock assignment</h1>
      <p className="mt-2 max-w-xl text-muted">
        Each app keeps its own secret. Two apps can share a design and still not share a key.
      </p>
      <div className="mt-5 flex flex-col gap-3 sm:flex-row">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search apps"
          className="h-11 flex-1 rounded-full border border-line bg-surface px-4"
        />
      </div>
      <div className="mt-3 flex gap-2 overflow-x-auto">
        {groups.map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setGroup(item)}
            className={`h-9 shrink-0 rounded-full px-3 text-sm ${item === group ? "bg-ink text-surface" : "bg-surface text-ink"}`}
          >
            {item}
          </button>
        ))}
      </div>
      <ul className="mt-4 flex flex-col gap-2">
        {visible.map((app) => {
          const assignment = assignments.find((item) => item.appId === app.id);
          return (
            <li key={app.id} className="rounded-2xl border border-line bg-surface p-3">
              <div className="flex items-center gap-3">
                <button type="button" onClick={() => onOpen(app.id)} className="flex min-w-0 flex-1 items-center gap-3 text-left">
                  <AppMark id={app.id} />
                  <span className="min-w-0">
                    <span className="block font-medium">{app.name}</span>
                    <span className="block truncate text-sm text-muted">
                      {assignment ? lockMeta(assignment.kind).name : app.blurb}
                    </span>
                  </span>
                </button>
                {assignment ? (
                  <Toggle
                    checked={assignment.armed}
                    label={`Lock ${app.name}`}
                    onChange={(value) => setArmed(app.id, value)}
                  />
                ) : null}
              </div>
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={() => onAssign(app.id)}
                  className="h-10 rounded-full bg-canvas px-4 text-sm font-semibold"
                >
                  {assignment ? "Change lock" : "Protect"}
                </button>
                <button type="button" onClick={() => onOpen(app.id)} className="h-10 rounded-full px-4 text-sm font-semibold text-accent">
                  Open
                </button>
                {assignment ? (
                  <button
                    type="button"
                    onClick={() => clearAssignment(app.id)}
                    className="h-10 rounded-full px-3 text-sm text-muted"
                  >
                    Remove
                  </button>
                ) : null}
              </div>
            </li>
          );
        })}
      </ul>
      {visible.length === 0 ? <p className="mt-6 text-sm text-muted">No app matches that search.</p> : null}
    </div>
  );
}

export function LocksView({ onConfigure }: { onConfigure: (kind: LockKind, master: boolean) => void }) {
  const assignments = useVault((state) => state.assignments);
  const master = useVault((state) => state.master);

  return (
    <div className="rise">
      <p className="text-xs font-semibold tracking-widest text-muted">LOCKS</p>
      <h1 className="mt-1 font-display text-4xl">Ten mechanisms</h1>
      <p className="mt-2 max-w-xl text-muted">
        Draw it, confirm it, then open it three times. That practice is required before anything arms.
      </p>
      <ul className="mt-6 grid gap-3 md:grid-cols-2">
        {LOCKS.map((lock) => {
          const count = kindInUse(lock.id, assignments, master);
          return (
            <li key={lock.id} className="flex flex-col rounded-2xl border border-line bg-surface p-4">
              <div className="flex items-center justify-between gap-3">
                <h2 className="font-display text-2xl">{lock.name}</h2>
                <span className="text-xs font-semibold tracking-widest text-muted">{lock.hardness}</span>
              </div>
              <p className="mt-1 text-xs font-semibold tracking-widest text-accent">{lock.kicker}</p>
              <p className="mt-2 flex-1 text-sm text-muted">{lock.summary}</p>
              <div className="mt-4 flex items-center justify-between">
                <span className="text-sm text-muted">{count === 0 ? "Unused" : `In use · ${count}`}</span>
                <button
                  type="button"
                  onClick={() => onConfigure(lock.id, false)}
                  className="h-10 rounded-full bg-ink px-4 text-sm font-semibold text-surface"
                >
                  Configure
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export function AlertsView() {
  const intruders = useVault((state) => state.intruders);
  const remove = useVault((state) => state.removeIntruder);
  const clear = useVault((state) => state.clearIntruders);

  return (
    <div className="rise">
      <div className="flex items-end justify-between gap-3">
        <div>
          <p className="text-xs font-semibold tracking-widest text-muted">ALERTS</p>
          <h1 className="mt-1 font-display text-4xl">Intruder log</h1>
        </div>
        {intruders.length > 0 ? (
          <button type="button" onClick={clear} className="text-sm font-semibold text-danger">
            Clear
          </button>
        ) : null}
      </div>
      <p className="mt-2 max-w-xl text-muted">
        After repeated misses, the studio tries a silent front-camera frame. Photos never leave this browser.
      </p>
      {intruders.length === 0 ? (
        <p className="mt-8 rounded-2xl border border-dashed border-line bg-surface px-4 py-10 text-center text-sm text-muted">
          No failed attempts on record.
        </p>
      ) : (
        <ul className="mt-6 grid gap-3 sm:grid-cols-2">
          {intruders.map((entry) => (
            <li key={entry.id} className="overflow-hidden rounded-2xl border border-line bg-surface">
              {entry.image ? (
                <img src={entry.image} alt="Intruder capture" className="h-36 w-full object-cover" />
              ) : (
                <div className="flex h-36 items-center justify-center bg-canvas text-sm text-muted">No photo</div>
              )}
              <div className="px-4 py-3">
                <p className="font-medium">
                  {appById(entry.appId)?.name ?? "Studio"} · {lockMeta(entry.kind).name}
                </p>
                <p className="text-sm text-muted">
                  {entry.attempts} misses · {formatWhen(entry.at)}
                </p>
                {entry.note ? <p className="mt-1 text-sm text-muted">{entry.note}</p> : null}
                <button type="button" onClick={() => remove(entry.id)} className="mt-2 text-sm font-semibold text-danger">
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

const GRACE = [
  { sec: 15, label: "15s" },
  { sec: 30, label: "30s" },
  { sec: 60, label: "1m" },
  { sec: 120, label: "2m" },
  { sec: 300, label: "5m" },
];

export function SettingsView({
  onMaster,
  onNote,
}: {
  onMaster: () => void;
  onNote: (text: string) => void;
}) {
  const settings = useVault((state) => state.settings);
  const master = useVault((state) => state.master);
  const recovery = useVault((state) => state.recovery);
  const update = useVault((state) => state.updateSettings);
  const setMaster = useVault((state) => state.setMaster);
  const setRecovery = useVault((state) => state.setRecovery);
  const resetAll = useVault((state) => state.resetAll);
  const [error, setError] = useState<string | null>(null);
  const [currentPin, setCurrentPin] = useState("");
  const [question, setQuestion] = useState(recovery?.question ?? "");
  const [answer, setAnswer] = useState("");
  const [pin, setPin] = useState("");
  const [phrase, setPhrase] = useState("");

  async function enroll() {
    setError(null);
    try {
      if (!biometricAvailable()) {
        setError("This browser has no platform authenticator. The lock designs still work.");
        return;
      }
      const credentialId = settings.credentialId ?? (await enrollBiometric());
      update({ biometric: true, credentialId });
      onNote("Biometric bypass is on for this device.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not enroll biometrics.");
    }
  }

  async function saveRecovery(event: React.FormEvent) {
    event.preventDefault();
    if (!recovery) return;
    if (!/^\d{6}$/.test(pin) || normalizeAnswer(answer).length < 2 || question.trim().length < 8) {
      setError("Question, answer, and a new 6-digit PIN are required.");
      return;
    }
    const pinOk = await matches(currentPin, recovery.pinHash);
    if (!pinOk) {
      setError("Current recovery PIN doesn’t match.");
      return;
    }
    const answerHash = await seal(normalizeAnswer(answer));
    const pinHash = await seal(pin);
    setRecovery({ question: question.trim(), answerHash, pinHash });
    setAnswer("");
    setPin("");
    setCurrentPin("");
    onNote("Recovery updated.");
  }

  async function erase(event: React.FormEvent) {
    event.preventDefault();
    if (!recovery) return;
    if (phrase !== "ERASE") {
      setError("Type ERASE to confirm.");
      return;
    }
    const pinOk = await matches(currentPin, recovery.pinHash);
    if (!pinOk) {
      setError("Current recovery PIN doesn’t match.");
      return;
    }
    resetAll();
  }

  return (
    <div className="rise max-w-2xl">
      <p className="text-xs font-semibold tracking-widest text-muted">SETTINGS</p>
      <h1 className="mt-1 font-display text-4xl">Control</h1>

      <section className="mt-6 rounded-2xl border border-line bg-surface p-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="font-semibold">Protection</h2>
            <p className="text-sm text-muted">Global switch. The studio lock still has to be passed before you can reach this.</p>
          </div>
          <Toggle checked={settings.armed} label="Protection" onChange={(armed) => update({ armed })} />
        </div>
      </section>

      <section className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <h2 className="font-semibold">Relock</h2>
        <p className="mt-1 text-sm text-muted">
          Immediate locks again as soon as you leave. Screen-off waits until the display sleeps. Grace allows a short return.
        </p>
        <div className="mt-3 grid gap-2 sm:grid-cols-3">
          {(
            [
              ["immediate", "Immediately"],
              ["screenoff", "Screen off"],
              ["grace", "Grace period"],
            ] as Array<[RelockMode, string]>
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              onClick={() => update({ relock: id })}
              className={`h-11 rounded-full text-sm font-semibold ${
                settings.relock === id ? "bg-ink text-surface" : "bg-canvas text-ink"
              }`}
            >
              {label}
            </button>
          ))}
        </div>
        {settings.relock === "grace" ? (
          <div className="mt-3 flex flex-wrap gap-2">
            {GRACE.map((item) => (
              <button
                key={item.sec}
                type="button"
                onClick={() => update({ graceSec: item.sec })}
                className={`h-9 rounded-full px-3 text-sm ${
                  settings.graceSec === item.sec ? "bg-accent text-accent-ink" : "bg-canvas"
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        ) : null}
        <p className="mt-3 text-sm text-muted">The power button in the header simulates the screen turning off.</p>
      </section>

      <section className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="flex items-center gap-2 font-semibold">
              <Shield size={16} aria-hidden="true" /> Studio lock
            </h2>
            <p className="mt-1 text-sm text-muted">
              {master ? `${lockMeta(master.kind).name} protects this studio.` : "No inner lock yet."}
            </p>
          </div>
        </div>
        <div className="mt-3 flex gap-2">
          <button type="button" onClick={onMaster} className="h-10 rounded-full bg-ink px-4 text-sm font-semibold text-surface">
            {master ? "Replace" : "Set studio lock"}
          </button>
          {master ? (
            <button type="button" onClick={() => setMaster(null)} className="h-10 rounded-full px-4 text-sm text-muted">
              Remove
            </button>
          ) : null}
        </div>
      </section>

      <section className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="font-semibold">Biometric bypass</h2>
            <p className="text-sm text-muted">Optional. The drawn lock stays as the fallback. Uses this device’s authenticator.</p>
          </div>
          <Toggle
            checked={settings.biometric}
            label="Biometric bypass"
            onChange={(value) => {
              if (value) void enroll();
              else update({ biometric: false });
            }}
          />
        </div>
      </section>

      <section className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <h2 className="font-semibold">Intruder threshold</h2>
        <div className="mt-3 flex gap-2">
          {[2, 3, 5].map((count) => (
            <button
              key={count}
              type="button"
              onClick={() => update({ intruderAfter: count })}
              className={`h-10 rounded-full px-4 text-sm font-semibold ${
                settings.intruderAfter === count ? "bg-ink text-surface" : "bg-canvas"
              }`}
            >
              {count} misses
            </button>
          ))}
        </div>
      </section>

      <section className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <h2 className="font-semibold">Battery guard</h2>
        <p className="mt-1 text-sm text-muted">
          While this tab is hidden and relock is set to screen-off, grants clear and timers stop. The Android build uses a
          lightweight foreground service that sleeps when the display is off, instead of polling in the background.
        </p>
      </section>

      <form onSubmit={(event) => void saveRecovery(event)} className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <h2 className="font-semibold">Recovery</h2>
        <div className="mt-3 grid gap-2">
          <input value={currentPin} onChange={(event) => setCurrentPin(event.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="Current PIN" inputMode="numeric" className="h-11 rounded-xl border border-line px-3" />
          <input value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="New question" className="h-11 rounded-xl border border-line px-3" />
          <input value={answer} onChange={(event) => setAnswer(event.target.value)} placeholder="New answer" autoComplete="off" className="h-11 rounded-xl border border-line px-3" />
          <input value={pin} onChange={(event) => setPin(event.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="New 6-digit PIN" inputMode="numeric" className="h-11 rounded-xl border border-line px-3" />
          <button type="submit" className="h-11 rounded-full bg-canvas text-sm font-semibold">
            Update recovery
          </button>
        </div>
      </form>

      <section className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <h2 className="flex items-center gap-2 font-semibold">
          <KeyRound size={16} aria-hidden="true" /> About
        </h2>
        <p className="mt-2 text-sm text-muted">
          Custom AppLock Studio · preview 1.0. Ad-free. Secrets are salted and hashed in this browser. The downloadable
          project is the Android architecture: Kotlin, Jetpack Compose, MVVM, Hilt, Room, an accessibility watcher, and a
          foreground service. No ad SDK.
        </p>
        <div className="mt-3 flex flex-wrap gap-2">
          <a
            href="/downloads/CustomAppLockStudio.zip"
            download
            className="inline-flex h-10 items-center rounded-full bg-ink px-4 text-sm font-semibold text-surface"
          >
            Download Android project
          </a>
          <button
            type="button"
            onClick={() => onNote("A Play Store link appears after the Android build is published. This preview has no listing.")}
            className="h-10 rounded-full bg-canvas px-4 text-sm font-semibold"
          >
            Rate
          </button>
        </div>
        <details className="mt-4 text-sm text-muted">
          <summary className="cursor-pointer font-semibold text-ink">Privacy</summary>
          <p className="mt-2">
            There is no account and no server. Lock secrets, recovery, and intruder frames stay in local storage on this
            device. Camera and notifications are optional. Denying them does not break the locks. Erasing the studio
            deletes the local record. The hold-to-erase control on a lock screen is the emergency exit if recovery is
            forgotten — it removes protection rather than trapping you.
          </p>
        </details>
      </section>

      <form onSubmit={(event) => void erase(event)} className="mt-3 rounded-2xl border border-line bg-surface p-4">
        <h2 className="font-semibold text-danger">Erase studio</h2>
        <p className="mt-1 text-sm text-muted">Removes every lock, alert, and the recovery seal.</p>
        <div className="mt-3 flex flex-col gap-2">
          <input
            value={currentPin}
            onChange={(event) => setCurrentPin(event.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="Current PIN"
            inputMode="numeric"
            className="h-11 rounded-xl border border-line px-3"
          />
          <div className="flex flex-col gap-2 sm:flex-row">
            <input value={phrase} onChange={(event) => setPhrase(event.target.value)} placeholder="Type ERASE" className="h-11 flex-1 rounded-xl border border-line px-3" />
            <button type="submit" className="h-11 rounded-full bg-danger px-4 text-sm font-semibold text-danger-soft">
              Erase
            </button>
          </div>
        </div>
      </form>
      {error ? (
        <p className="mt-3 text-sm text-danger" role="status">
          {error}
        </p>
      ) : null}
    </div>
  );
}
