import type { SlaState, Status } from "@/types/ticket";

interface SlaFuseProps {
  slaState: SlaState;
  status: Status;
  dueAt: string;
  resolvedAt: string | null;
  /** "row" = compact bar for list rows, "detail" = larger with label for the ticket page */
  variant?: "row" | "detail";
}

const STATE_STYLES: Record<SlaState, { bar: string; text: string; label: string }> = {
  ON_TRACK: { bar: "bg-teal", text: "text-teal", label: "On track" },
  AT_RISK: { bar: "bg-amber-DEFAULT", text: "text-amber-DEFAULT", label: "At risk" },
  BREACHED: { bar: "bg-coral", text: "text-coral", label: "Breached" },
};

/**
 * The app's signature visual element: a small horizontal "fuse" representing
 * this ticket's SLA window, derived directly from the backend's own
 * ON_TRACK / AT_RISK / BREACHED calculation - not a decorative progress bar.
 * For finished tickets (resolved/closed) it shows a solid, filled bar since
 * the window is no longer counting down.
 */
export default function SlaFuse({ slaState, status, dueAt, resolvedAt, variant = "row" }: SlaFuseProps) {
  const style = STATE_STYLES[slaState];
  const isFinished = status === "RESOLVED" || status === "CLOSED";
  const fillPercent = isFinished ? 100 : fillPercentFromNow(dueAt);

  const caption = isFinished
    ? slaState === "BREACHED"
      ? `Resolved past deadline`
      : `Resolved within SLA`
    : formatRelativeDue(dueAt);

  if (variant === "row") {
    return (
      <div className="flex w-24 flex-col gap-1" title={`${style.label} · ${caption}`}>
        <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-200">
          <div
            className={`h-full rounded-full ${style.bar} transition-all`}
            style={{ width: `${fillPercent}%` }}
          />
        </div>
        <span className={`font-mono text-[11px] leading-none ${style.text}`}>{caption}</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between">
        <span className={`text-sm font-medium font-body ${style.text}`}>{style.label}</span>
        <span className="font-mono text-xs text-slate-500">
          {resolvedAt ? "resolved" : "due"} {isFinished ? "" : formatRelativeDue(dueAt)}
        </span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
        <div className={`h-full rounded-full ${style.bar} transition-all`} style={{ width: `${fillPercent}%` }} />
      </div>
    </div>
  );
}

function fillPercentFromNow(dueAtIso: string): number {
  const dueAt = new Date(dueAtIso).getTime();
  const now = Date.now();
  if (now >= dueAt) return 100;
  // We don't know createdAt here, so approximate remaining-time pressure over
  // a rolling 24h window for the bar fill - the color (from slaState) is what
  // actually communicates urgency; the bar is a supporting visual, not the
  // primary signal.
  const windowMs = 24 * 60 * 60 * 1000;
  const remainingMs = dueAt - now;
  const consumed = Math.max(0, Math.min(1, 1 - remainingMs / windowMs));
  return Math.round(consumed * 100);
}

function formatRelativeDue(dueAtIso: string): string {
  const dueAt = new Date(dueAtIso).getTime();
  const now = Date.now();
  const diffMs = dueAt - now;
  const diffHours = Math.round(Math.abs(diffMs) / (60 * 60 * 1000));
  const diffMinutes = Math.round(Math.abs(diffMs) / (60 * 1000));

  if (diffMs <= 0) {
    return diffHours >= 1 ? `${diffHours}h overdue` : `${diffMinutes}m overdue`;
  }
  return diffHours >= 1 ? `${diffHours}h left` : `${diffMinutes}m left`;
}
