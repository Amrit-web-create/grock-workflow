import {
  AppWindow,
  Calendar,
  Camera,
  Clapperboard,
  Cloud,
  Compass,
  Folder,
  Image,
  Landmark,
  Mail,
  Map,
  MessageSquare,
  MessagesSquare,
  Music,
  NotebookPen,
  Wallet,
  type LucideIcon,
} from "lucide-react";

const ICONS: Record<string, LucideIcon> = {
  relay: MessageSquare,
  frames: Image,
  ledger: Landmark,
  post: Mail,
  atlas: Folder,
  north: Map,
  pulse: Music,
  field: NotebookPen,
  daybook: Calendar,
  lens: Camera,
  till: Wallet,
  harbor: Compass,
  keep: Cloud,
  cinema: Clapperboard,
  forum: MessagesSquare,
};

export function AppMark({ id }: { id: string }) {
  const Icon = ICONS[id] ?? AppWindow;
  return (
    <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-canvas text-ink">
      <Icon size={18} strokeWidth={1.75} aria-hidden="true" />
    </span>
  );
}

export function formatWhen(at: number): string {
  const delta = Date.now() - at;
  if (delta < 60_000) return "Just now";
  if (delta < 3_600_000) return `${Math.floor(delta / 60_000)} min ago`;
  return new Date(at).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}
