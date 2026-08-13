import { useNavigate } from "react-router-dom";
import { LogOut, Menu } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";

interface TopbarProps {
  title: string;
  onMenuClick: () => void;
}

export default function Topbar({ title, onMenuClick }: TopbarProps) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-200 bg-paper-0 px-4 sm:px-6">
      <div className="flex min-w-0 items-center gap-3">
        <button
          onClick={onMenuClick}
          aria-label="Open menu"
          className="-ml-1 rounded-md p-1.5 text-slate-500 hover:bg-paper-50 hover:text-ink-950 lg:hidden"
        >
          <Menu size={20} />
        </button>
        <h1 className="truncate font-display text-lg font-semibold text-ink-950">{title}</h1>
      </div>
      <button
        onClick={handleLogout}
        className="flex shrink-0 items-center gap-2 rounded-lg px-2.5 py-1.5 text-sm font-medium font-body text-slate-500 transition-colors hover:bg-paper-50 hover:text-ink-950 sm:px-3"
      >
        <LogOut size={15} />
        <span className="hidden sm:inline">Log out</span>
      </button>
    </header>
  );
}
