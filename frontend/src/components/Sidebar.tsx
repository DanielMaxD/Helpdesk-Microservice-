import { NavLink } from "react-router-dom";
import { LayoutDashboard, Ticket, PlusCircle, Users, Bell, LifeBuoy, X, type LucideIcon } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import type { Role } from "@/types/auth";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  roles: Role[];
}

interface SidebarProps {
  /** Whether the mobile drawer is open. Ignored at the `lg` breakpoint and above, where the sidebar is always visible. */
  isOpen: boolean;
  onClose: () => void;
}

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, roles: ["USER", "AGENT", "ADMIN"] },
  { to: "/tickets", label: "Tickets", icon: Ticket, roles: ["USER", "AGENT", "ADMIN"] },
  { to: "/tickets/new", label: "New ticket", icon: PlusCircle, roles: ["USER"] },
  { to: "/notifications", label: "Notifications", icon: Bell, roles: ["USER", "AGENT", "ADMIN"] },
  { to: "/admin/users", label: "Users", icon: Users, roles: ["ADMIN"] },
];

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { user } = useAuth();
  if (!user) return null;

  const items = NAV_ITEMS.filter((item) => item.roles.includes(user.role));

  return (
    <>
      {/* Mobile backdrop — only rendered (and interactive) while the drawer is open */}
      {isOpen && (
        <div
          onClick={onClose}
          aria-hidden="true"
          className="fixed inset-0 z-30 bg-ink-950/50 lg:hidden"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex h-screen w-64 shrink-0 flex-col bg-ink-950 text-paper-50 transition-transform duration-200 ease-out lg:static lg:z-auto lg:w-60 lg:translate-x-0 ${
          isOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between px-5 py-5">
          <div className="flex items-center gap-2">
            <LifeBuoy size={22} className="text-indigo-100" strokeWidth={2.25} />
            <span className="font-display text-lg font-semibold tracking-tight text-white">HelpDesk</span>
          </div>
          <button
            onClick={onClose}
            aria-label="Close menu"
            className="rounded-md p-1 text-slate-400 hover:bg-white/5 hover:text-white lg:hidden"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-3 py-2">
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/" || to === "/tickets"}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium font-body transition-colors ${
                  isActive
                    ? "bg-white/10 text-white"
                    : "text-slate-400 hover:bg-white/5 hover:text-paper-50"
                }`
              }
            >
              <Icon size={17} />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-white/10 px-5 py-4">
          <p className="font-body text-xs text-slate-400">Signed in as</p>
          <p className="truncate font-body text-sm font-medium text-white">{user.name}</p>
          <span className="mt-1 inline-block rounded-full bg-white/10 px-2 py-0.5 font-mono text-[10px] uppercase tracking-wide text-indigo-100">
            {user.role}
          </span>
        </div>
      </aside>
    </>
  );
}
