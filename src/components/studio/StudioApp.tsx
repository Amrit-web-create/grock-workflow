import { useEffect, useState } from "react";
import { Bell, KeyRound, LayoutGrid, Power, Settings, Shield } from "lucide-react";
import { appById } from "@/lib/vault/catalog";
import { clearGrants, grantDuration, grantFor, hasGrant } from "@/lib/vault/grants";
import { lockMeta } from "@/lib/vault/locks";
import { newId, useVault } from "@/lib/vault/store";
import type { LockKind } from "@/lib/vault/types";
import { Onboarding } from "@/components/studio/Onboarding";
import { LockOverlay } from "@/components/studio/Overlay";
import { AlertsView, AppsView, LocksView, SettingsView, VaultView } from "@/components/studio/Views";
import { Wizard } from "@/components/studio/Wizard";
import { AppMark } from "@/components/studio/marks";

type ViewId = "vault" | "apps" | "locks" | "alerts" | "settings";

const NAV: Array<{ id: ViewId; label: string; icon: typeof Shield }> = [
  { id: "vault", label: "Vault", icon: Shield },
  { id: "apps", label: "Apps", icon: LayoutGrid },
  { id: "locks", label: "Locks", icon: KeyRound },
  { id: "alerts", label: "Alerts", icon: Bell },
  { id: "settings", label: "Settings", icon: Settings },
];

export function StudioApp() {
  const hydrated = useVault((state) => state.hydrated);
  const hydrateError = useVault((state) => state.hydrateError);
  const onboarded = useVault((state) => state.onboarded);
  const settings = useVault((state) => state.settings);
  const master = useVault((state) => state.master);
  const [view, setView] = useState<ViewId>("vault");
  const [wizard, setWizard] = useState<{ kind: LockKind; apps: string[]; master: boolean } | null>(null);
  const [locking, setLocking] = useState<string | null>(null);
  const [opened, setOpened] = useState<string | null>(null);
  const [screenOff, setScreenOff] = useState(false);
  const [pulse, setPulse] = useState(0);
  const [notice, setNotice] = useState<string | null>(null);
  const [pickKind, setPickKind] = useState<string[] | null>(null);

  useEffect(() => {
    let alive = true;
    const finish = () => {
      if (!alive) return;
      if (!useVault.getState().hydrated) useVault.setState({ hydrated: true });
    };
    const pending = useVault.persist.rehydrate();
    if (pending && typeof (pending as Promise<void>).then === "function") {
      void (pending as Promise<void>).then(finish, () => {
        useVault.setState({
          hydrated: true,
          hydrateError: "Saved locks could not be read. Erase this studio from a lock screen if you are stuck.",
        });
      });
    } else finish();
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    if (!notice) return;
    const id = window.setTimeout(() => setNotice(null), 3200);
    return () => window.clearTimeout(id);
  }, [notice]);

  useEffect(() => {
    const onVis = () => {
      if (document.hidden && useVault.getState().settings.relock === "screenoff") {
        clearGrants();
        setOpened(null);
        setLocking(null);
        setScreenOff(true);
        setPulse((value) => value + 1);
      }
    };
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, []);

  function power() {
    if (useVault.getState().settings.relock === "screenoff") clearGrants();
    setOpened(null);
    setLocking(null);
    setScreenOff(true);
    setPulse((value) => value + 1);
  }

  function openApp(appId: string) {
    const state = useVault.getState();
    const assignment = state.assignments.find((item) => item.appId === appId && item.armed);
    if (!state.settings.armed || !assignment || hasGrant(appId)) {
      setOpened(appId);
      return;
    }
    setLocking(appId);
  }

  function unlockedApp(appId: string) {
    const state = useVault.getState();
    const assignment = state.assignments.find((item) => item.appId === appId);
    grantFor(appId, grantDuration(state.settings.relock, state.settings.graceSec, false));
    if (assignment) {
      state.addActivity({ id: newId(), at: Date.now(), appId, kind: assignment.kind, ok: true });
    }
    setLocking(null);
    setOpened(appId);
    setPulse((value) => value + 1);
  }

  if (!onboarded) return <Onboarding />;

  if (wizard) {
    return (
      <Wizard
        kind={wizard.kind}
        initialApps={wizard.apps}
        master={wizard.master}
        onClose={() => setWizard(null)}
        onSaved={setNotice}
      />
    );
  }

  const showMaster =
    settings.armed && settings.masterOn && master && !hasGrant("master") ? master : null;
  void pulse;

  const lockingAssignment = locking
    ? useVault.getState().assignments.find((item) => item.appId === locking && item.armed)
    : undefined;

  return (
    <div className="min-h-screen bg-canvas text-ink md:flex">
      <aside className="hidden w-60 shrink-0 border-r border-line bg-surface md:flex md:flex-col md:px-4 md:py-6">
        <p className="px-3 text-xs font-semibold tracking-widest text-muted">STUDIO</p>
        <p className="px-3 font-display text-2xl">AppLock</p>
        <nav className="mt-8 flex flex-col gap-1">
          {NAV.map((item) => {
            const Icon = item.icon;
            const active = view === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setView(item.id)}
                className={`flex h-11 items-center gap-3 rounded-xl px-3 text-left text-sm font-medium ${
                  active ? "bg-canvas text-ink" : "text-muted"
                }`}
              >
                <Icon size={18} strokeWidth={1.75} aria-hidden="true" />
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>
      <div className="min-w-0">
        <header className="flex h-14 items-center justify-between px-4 md:px-8">
          <p className="font-display text-xl md:hidden">AppLock</p>
          <p className="hidden text-sm text-muted md:block">
            {settings.armed ? "Armed" : "Disarmed"}
            {settings.relock === "grace" ? ` · ${settings.graceSec}s grace` : ""}
          </p>
          <button type="button" onClick={power} className="flex h-10 items-center gap-2 rounded-full px-3 text-sm font-semibold" aria-label="Turn screen off">
            <Power size={16} aria-hidden="true" />
            Screen
          </button>
        </header>
        <main className="px-4 pb-24 md:px-8 md:pb-12">
          {hydrated && hydrateError ? (
            <p className="mb-4 rounded-2xl bg-danger-soft px-4 py-3 text-sm text-danger">{hydrateError}</p>
          ) : null}
          {view === "vault" ? <VaultView onOpen={openApp} onBrowse={() => setView("apps")} /> : null}
          {view === "apps" ? (
            <AppsView
              onOpen={openApp}
              onAssign={(appId) => setPickKind([appId])}
            />
          ) : null}
          {view === "locks" ? (
            <LocksView onConfigure={(kind) => setWizard({ kind, apps: [], master: false })} />
          ) : null}
          {view === "alerts" ? <AlertsView /> : null}
          {view === "settings" ? (
            <SettingsView
              onNote={setNotice}
              onMaster={() => setPickKind([])}
            />
          ) : null}
        </main>
      </div>
      <nav className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 border-t border-line bg-surface md:hidden">
        {NAV.map((item) => {
          const Icon = item.icon;
          const active = view === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setView(item.id)}
              className={`flex h-16 flex-col items-center justify-center gap-1 text-xs ${active ? "text-ink" : "text-muted"}`}
            >
              <Icon size={18} strokeWidth={1.75} aria-hidden="true" />
              {item.label}
            </button>
          );
        })}
      </nav>

      {pickKind ? (
        <KindSheet
          master={pickKind.length === 0 && view === "settings"}
          onClose={() => setPickKind(null)}
          onPick={(kind) => {
            const apps = pickKind;
            const asMaster = apps.length === 0 && view === "settings";
            setPickKind(null);
            setWizard({ kind, apps, master: asMaster });
          }}
        />
      ) : null}

      {opened ? <AppStage appId={opened} onClose={() => setOpened(null)} /> : null}

      {locking && lockingAssignment ? (
        <LockOverlay
          title={appById(locking)?.name ?? "App"}
          detail={lockMeta(lockingAssignment.kind).name}
          kind={lockingAssignment.kind}
          sealed={lockingAssignment.sealed}
          appId={locking}
          cancellable
          onUnlocked={() => unlockedApp(locking)}
          onCancel={() => setLocking(null)}
        />
      ) : null}

      {showMaster ? (
        <LockOverlay
          title="Studio"
          detail={lockMeta(showMaster.kind).name}
          kind={showMaster.kind}
          sealed={showMaster.sealed}
          appId="master"
          cancellable={false}
          onUnlocked={() => {
            grantFor("master", grantDuration(settings.relock, settings.graceSec, true));
            setPulse((value) => value + 1);
            setNotice("Studio unlocked.");
          }}
          onCancel={() => undefined}
        />
      ) : null}

      {screenOff ? (
        <button
          type="button"
          onClick={() => setScreenOff(false)}
          className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-night text-mist"
        >
          <span className="text-xs tracking-widest">DISPLAY OFF</span>
          <span className="mt-3 font-display text-3xl text-ivory">Tap to wake</span>
        </button>
      ) : null}

      {notice ? (
        <p className="fixed bottom-20 left-4 right-4 z-40 mx-auto max-w-md rounded-full bg-ink px-4 py-3 text-center text-sm text-surface md:bottom-6" role="status">
          {notice}
        </p>
      ) : null}
    </div>
  );
}

function KindSheet({
  master,
  onClose,
  onPick,
}: {
  master: boolean;
  onClose: () => void;
  onPick: (kind: LockKind) => void;
}) {
  const kinds = ["lattice", "cipher", "tumblers", "chroma", "glyph", "chrono", "signal", "formula", "orbit", "vector"] as const;
  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-night/40 md:items-center">
      <div className="max-h-screen w-full max-w-lg overflow-auto rounded-t-3xl bg-surface p-4 md:rounded-3xl">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-2xl">{master ? "Studio lock" : "Choose a design"}</h2>
          <button type="button" onClick={onClose} className="text-sm font-semibold text-muted">
            Close
          </button>
        </div>
        <ul className="mt-3 flex flex-col">
          {kinds.map((kind) => {
            const meta = lockMeta(kind);
            return (
              <li key={kind}>
                <button
                  type="button"
                  onClick={() => onPick(kind)}
                  className="flex w-full items-center justify-between gap-3 border-b border-line py-3 text-left"
                >
                  <span>
                    <span className="block font-medium">{meta.name}</span>
                    <span className="text-sm text-muted">{meta.kicker}</span>
                  </span>
                  <span className="text-xs text-muted">{meta.hardness}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

function AppStage({ appId, onClose }: { appId: string; onClose: () => void }) {
  const app = appById(appId);
  if (!app) return null;
  return (
    <div className="fixed inset-0 z-40 flex flex-col bg-canvas">
      <header className="flex h-14 items-center gap-3 border-b border-line bg-surface px-4">
        <button type="button" onClick={onClose} className="text-sm font-semibold text-accent">
          Close
        </button>
        <AppMark id={app.id} />
        <div>
          <p className="font-medium leading-tight">{app.name}</p>
          <p className="text-xs text-muted">{app.blurb}</p>
        </div>
      </header>
      <ul className="mx-auto flex w-full max-w-xl flex-col gap-2 px-4 py-6">
        {app.lines.map((line) => (
          <li key={line} className="rounded-2xl border border-line bg-surface px-4 py-4 text-sm">
            {line}
          </li>
        ))}
      </ul>
    </div>
  );
}
