import type { LucideIcon } from "lucide-react";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: React.ReactNode;
}

export default function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-slate-200 bg-paper-0 px-6 py-14 text-center">
      <Icon size={28} className="text-slate-400" strokeWidth={1.5} />
      <p className="font-body text-sm font-medium text-ink-950">{title}</p>
      {description && <p className="max-w-sm font-body text-sm text-slate-500">{description}</p>}
      {action}
    </div>
  );
}
