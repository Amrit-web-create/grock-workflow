import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";
import type {
  ActivityEntry,
  Assignment,
  IntruderEntry,
  LockKind,
  MasterLock,
  PersistedVault,
  Recovery,
  Settings,
} from "@/lib/vault/types";

const defaultSettings: Settings = {
  armed: true,
  relock: "grace",
  graceSec: 30,
  biometric: false,
  credentialId: null,
  intruderAfter: 3,
  masterOn: false,
};

interface VaultState extends PersistedVault {
  hydrated: boolean;
  hydrateError: string | null;
  setOnboarded: (recovery: Recovery) => void;
  updateSettings: (patch: Partial<Settings>) => void;
  setMaster: (master: MasterLock | null) => void;
  upsertAssignment: (assignment: Assignment) => void;
  clearAssignment: (appId: string) => void;
  setAssignmentArmed: (appId: string, armed: boolean) => void;
  addIntruder: (entry: IntruderEntry) => void;
  removeIntruder: (id: string) => void;
  clearIntruders: () => void;
  addActivity: (entry: ActivityEntry) => void;
  setRecovery: (recovery: Recovery) => void;
  resetAll: () => void;
}

const empty = (): PersistedVault => ({
  version: 1,
  onboarded: false,
  recovery: null,
  settings: defaultSettings,
  master: null,
  assignments: [],
  intruders: [],
  activity: [],
});

export const useVault = create<VaultState>()(
  persist(
    (set) => ({
      ...empty(),
      hydrated: false,
      hydrateError: null,
      setOnboarded: (recovery) => set({ onboarded: true, recovery }),
      updateSettings: (patch) =>
        set((state) => ({ settings: { ...state.settings, ...patch } })),
      setMaster: (master) =>
        set((state) => ({
          master,
          settings: { ...state.settings, masterOn: master !== null },
        })),
      upsertAssignment: (assignment) =>
        set((state) => ({
          assignments: [
            ...state.assignments.filter((item) => item.appId !== assignment.appId),
            assignment,
          ],
        })),
      clearAssignment: (appId) =>
        set((state) => ({
          assignments: state.assignments.filter((item) => item.appId !== appId),
        })),
      setAssignmentArmed: (appId, armed) =>
        set((state) => ({
          assignments: state.assignments.map((item) =>
            item.appId === appId ? { ...item, armed } : item,
          ),
        })),
      addIntruder: (entry) =>
        set((state) => ({ intruders: [entry, ...state.intruders].slice(0, 12) })),
      removeIntruder: (id) =>
        set((state) => ({ intruders: state.intruders.filter((item) => item.id !== id) })),
      clearIntruders: () => set({ intruders: [] }),
      addActivity: (entry) =>
        set((state) => ({ activity: [entry, ...state.activity].slice(0, 40) })),
      setRecovery: (recovery) => set({ recovery }),
      resetAll: () => {
        set({ ...empty(), hydrated: true, hydrateError: null });
      },
    }),
    {
      name: "applock-studio-v1",
      storage: createJSONStorage(() => localStorage),
      skipHydration: true,
      partialize: (state) => ({
        version: state.version,
        onboarded: state.onboarded,
        recovery: state.recovery,
        settings: state.settings,
        master: state.master,
        assignments: state.assignments,
        intruders: state.intruders,
        activity: state.activity,
      }),
      onRehydrateStorage: () => (_state, error) => {
        useVault.setState({
          hydrated: true,
          hydrateError: error ? "Saved locks could not be read. You can erase this studio and start again." : null,
        });
      },
    },
  ),
);

export function assignmentFor(appId: string): Assignment | undefined {
  return useVault.getState().assignments.find((item) => item.appId === appId);
}

export function newId(): string {
  return crypto.randomUUID();
}

export function kindInUse(kind: LockKind, assignments: Assignment[], master: MasterLock | null): number {
  return assignments.filter((item) => item.kind === kind).length + (master?.kind === kind ? 1 : 0);
}
