export interface CatalogApp {
  id: string;
  name: string;
  blurb: string;
  group: "Communicate" | "Create" | "Work" | "Finance" | "Travel";
  lines: string[];
}

export const CATALOG: CatalogApp[] = [
  {
    id: "relay",
    name: "Relay",
    blurb: "Messages",
    group: "Communicate",
    lines: [
      "North desk — The revised seal arrived. I left it in the drawer.",
      "A. Sen — Can you take the late train? Platform is quieter.",
      "Field office — Photos from the site are in Frames, album Shore.",
    ],
  },
  {
    id: "frames",
    name: "Frames",
    blurb: "Photos",
    group: "Create",
    lines: [
      "Shore · 48 studies in flat light",
      "Desk · contact sheets, unedited",
      "Night walk · sodium and glass",
    ],
  },
  {
    id: "ledger",
    name: "Ledger",
    blurb: "Banking",
    group: "Finance",
    lines: [
      "Operating · available balance held locally in this preview",
      "Transfer draft · not sent, waiting on you",
      "Card · ending 4418 · locked in the app, not at the bank",
    ],
  },
  {
    id: "post",
    name: "Post",
    blurb: "Mail",
    group: "Communicate",
    lines: [
      "Studio — Invoice 184 is attached. No reply needed.",
      "Press — The interview window is Thursday, twenty minutes.",
      "Archive — Last month’s letters, already filed.",
    ],
  },
  {
    id: "atlas",
    name: "Atlas",
    blurb: "Files",
    group: "Work",
    lines: [
      "Contracts / unsigned",
      "Drawings / lock-faces-v3",
      "Notes / recovery questions — do not put secrets here",
    ],
  },
  {
    id: "north",
    name: "North",
    blurb: "Maps",
    group: "Travel",
    lines: [
      "Saved pin · Reading room",
      "Saved pin · Night entrance, east stair",
      "Offline area · the old city, downloaded",
    ],
  },
  {
    id: "pulse",
    name: "Pulse",
    blurb: "Music",
    group: "Create",
    lines: [
      "Queue · piano studies, no vocals",
      "Made for this room · low volume",
      "Downloads · available with the screen off",
    ],
  },
  {
    id: "field",
    name: "Fieldnotes",
    blurb: "Notes",
    group: "Work",
    lines: [
      "Don’t reuse a banking PIN.",
      "Practice a lock three times before you trust it.",
      "Recovery lives only on this device.",
    ],
  },
  {
    id: "daybook",
    name: "Daybook",
    blurb: "Calendar",
    group: "Work",
    lines: [
      "Today · Deep work, 09:00–12:00",
      "Today · Walk the long way home",
      "Friday · Renew the studio certificate",
    ],
  },
  {
    id: "lens",
    name: "Lens",
    blurb: "Camera",
    group: "Create",
    lines: [
      "Intruder capture uses this permission only after repeated failures.",
      "Nothing is uploaded.",
      "Shots stay in the intruder log until you delete them.",
    ],
  },
  {
    id: "till",
    name: "Till",
    blurb: "Wallet",
    group: "Finance",
    lines: [
      "Transit card · ready",
      "Studio pass · expires in 40 days",
      "Receipts · four this week, all local",
    ],
  },
  {
    id: "harbor",
    name: "Harbor",
    blurb: "Browser",
    group: "Work",
    lines: [
      "Reading list is empty.",
      "No account. No ads. No remote lock-in.",
      "Close the tab and the grace timer still runs.",
    ],
  },
  {
    id: "keep",
    name: "Keephouse",
    blurb: "Cloud drive",
    group: "Work",
    lines: [
      "This preview does not sync.",
      "On a phone, files would stay in private storage.",
      "Treat the downloadable project as the source of truth.",
    ],
  },
  {
    id: "cinema",
    name: "Cinema",
    blurb: "Video",
    group: "Create",
    lines: [
      "Continue · lecture, 14 minutes left",
      "Downloads · two films for the train",
      "History is stored on this device only",
    ],
  },
  {
    id: "forum",
    name: "Forum",
    blurb: "Community",
    group: "Communicate",
    lines: [
      "Thread · quiet rooms, no notifications after midnight",
      "Draft · unsent",
      "Muted · three conversations",
    ],
  },
];

export function appById(id: string): CatalogApp | undefined {
  return CATALOG.find((app) => app.id === id);
}
