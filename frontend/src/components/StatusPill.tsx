import type { Status } from "@/types/ticket";

const STATUS_STYLES: Record<Status, string> = {
  OPEN: "bg-slate-200/60 text-slate-600",
  IN_PROGRESS: "bg-indigo-100 text-indigo-600",
  RESOLVED: "bg-teal-100 text-teal",
  CLOSED: "bg-ink-900/5 text-slate-500",
};

const STATUS_LABELS: Record<Status, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

export default function StatusPill({ status }: { status: Status }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium font-body ${STATUS_STYLES[status]}`}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}
