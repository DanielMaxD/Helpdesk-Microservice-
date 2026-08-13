interface StatCardProps {
  label: string;
  value: number;
  accent?: "default" | "teal" | "amber" | "coral";
}

const ACCENT_STYLES: Record<NonNullable<StatCardProps["accent"]>, string> = {
  default: "text-ink-950",
  teal: "text-teal",
  amber: "text-amber-DEFAULT",
  coral: "text-coral",
};

export default function StatCard({ label, value, accent = "default" }: StatCardProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-paper-0 p-5 shadow-card">
      <p className="font-body text-sm text-slate-500">{label}</p>
      <p className={`mt-2 font-display text-3xl font-semibold ${ACCENT_STYLES[accent]}`}>{value}</p>
    </div>
  );
}
