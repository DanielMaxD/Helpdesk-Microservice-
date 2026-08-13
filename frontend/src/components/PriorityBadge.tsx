import type { Priority } from "@/types/ticket";

const PRIORITY_STYLES: Record<Priority, string> = {
  LOW: "bg-slate-200/60 text-slate-600",
  MEDIUM: "bg-indigo-100 text-indigo-600",
  HIGH: "bg-amber-100 text-amber-DEFAULT",
  CRITICAL: "bg-coral-100 text-coral",
};

const PRIORITY_LABELS: Record<Priority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

export default function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium font-body ${PRIORITY_STYLES[priority]}`}
    >
      {PRIORITY_LABELS[priority]}
    </span>
  );
}
