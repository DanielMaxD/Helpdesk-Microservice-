import { Check } from "lucide-react";
import type { Status } from "@/types/ticket";

const STEPS: Status[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];
const LABELS: Record<Status, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

/**
 * A real sequence marker, not decorative: OPEN -> IN_PROGRESS -> RESOLVED ->
 * CLOSED is the ticket's actual, backend-enforced lifecycle.
 */
export default function StatusStepper({ current }: { current: Status }) {
  const currentIndex = STEPS.indexOf(current);

  return (
    <div className="flex items-center">
      {STEPS.map((step, i) => {
        const isDone = i < currentIndex;
        const isCurrent = i === currentIndex;
        return (
          <div key={step} className="flex items-center">
            <div className="flex flex-col items-center gap-1.5">
              <div
                className={`flex h-7 w-7 items-center justify-center rounded-full font-mono text-xs font-medium transition-colors ${
                  isDone
                    ? "bg-teal text-white"
                    : isCurrent
                    ? "bg-indigo text-white"
                    : "bg-slate-200 text-slate-500"
                }`}
              >
                {isDone ? <Check size={14} /> : i + 1}
              </div>
              <span className={`font-body text-[11px] ${isCurrent ? "font-medium text-ink-950" : "text-slate-400"}`}>
                {LABELS[step]}
              </span>
            </div>
            {i < STEPS.length - 1 && (
              <div className={`mx-2 mb-4 h-0.5 w-8 sm:w-12 ${i < currentIndex ? "bg-teal" : "bg-slate-200"}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}
