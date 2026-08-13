import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Inbox, PlusCircle, Search } from "lucide-react";
import { ticketApi } from "@/api/ticketApi";
import { getErrorMessage } from "@/api/client";
import { useAuth } from "@/hooks/useAuth";
import { useUserDirectory } from "@/hooks/useUserDirectory";
import { usePageTitle } from "@/layouts/AppLayout";
import type { Status, Ticket } from "@/types/ticket";
import PriorityBadge from "@/components/PriorityBadge";
import StatusPill from "@/components/StatusPill";
import SlaFuse from "@/components/SlaFuse";
import ErrorBanner from "@/components/ErrorBanner";
import EmptyState from "@/components/EmptyState";
import FullScreenSpinner from "@/components/FullScreenSpinner";
import { formatDate } from "@/utils/format";

const STATUS_FILTERS: Array<{ label: string; value: Status | "ALL" }> = [
  { label: "All", value: "ALL" },
  { label: "Open", value: "OPEN" },
  { label: "In progress", value: "IN_PROGRESS" },
  { label: "Resolved", value: "RESOLVED" },
  { label: "Closed", value: "CLOSED" },
];

export default function TicketListPage() {
  usePageTitle("Tickets");
  const { user } = useAuth();
  const directory = useUserDirectory();

  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<Status | "ALL">("ALL");
  const [query, setQuery] = useState("");

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    setIsLoading(true);

    const fetcher = user.role === "ADMIN" ? ticketApi.getAll : user.role === "AGENT" ? ticketApi.getAssigned : ticketApi.getMy;

    fetcher()
      .then((data) => {
        if (!cancelled) setTickets(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, "Couldn't load tickets."));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [user]);

  const filtered = useMemo(() => {
    return tickets
      .filter((t) => statusFilter === "ALL" || t.status === statusFilter)
      .filter((t) => t.title.toLowerCase().includes(query.trim().toLowerCase()))
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }, [tickets, statusFilter, query]);

  const scopeLabel = user?.role === "ADMIN" ? "All tickets" : user?.role === "AGENT" ? "Assigned to you" : "Your tickets";

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-display text-xl font-semibold text-ink-950">{scopeLabel}</h2>
          <p className="mt-1 font-body text-sm text-slate-500">{filtered.length} of {tickets.length} tickets shown</p>
        </div>
        {user?.role === "USER" && (
          <Link
            to="/tickets/new"
            className="flex items-center gap-2 rounded-lg bg-indigo px-4 py-2 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600"
          >
            <PlusCircle size={16} />
            New ticket
          </Link>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by title…"
            className="w-56 rounded-lg border border-slate-200 bg-paper-0 py-2 pl-9 pr-3 font-body text-sm text-ink-950 placeholder:text-slate-400 focus:border-indigo focus:outline-none focus:ring-2 focus:ring-indigo-100"
          />
        </div>
        <div className="flex gap-1 rounded-lg border border-slate-200 bg-paper-0 p-1">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setStatusFilter(f.value)}
              className={`rounded-md px-3 py-1.5 font-body text-xs font-medium transition-colors ${
                statusFilter === f.value ? "bg-indigo text-white" : "text-slate-500 hover:bg-paper-50"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {error && <ErrorBanner message={error} />}

      {isLoading ? (
        <FullScreenSpinner />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={Inbox}
          title="No tickets found"
          description={tickets.length === 0 ? "Nothing here yet." : "Try a different filter or search term."}
        />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-paper-0 shadow-card">
          <table className="w-full min-w-[720px] text-left">
            <thead>
              <tr className="border-b border-slate-200 bg-paper-50 font-body text-xs uppercase tracking-wide text-slate-500">
                <th className="px-4 py-3 font-medium">Ticket</th>
                <th className="px-4 py-3 font-medium">Priority</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">SLA</th>
                <th className="px-4 py-3 font-medium">Created</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((ticket) => (
                <tr key={ticket.id} className="border-b border-slate-200 last:border-0 hover:bg-paper-50">
                  <td className="px-4 py-3">
                    <Link to={`/tickets/${ticket.id}`} className="font-body text-sm font-medium text-ink-950 hover:text-indigo">
                      {ticket.title}
                    </Link>
                    <p className="mt-0.5 font-mono text-[11px] text-slate-400">
                      #{ticket.id.slice(0, 8)}
                      {user?.role === "ADMIN" && (
                        <>
                          {" "}
                          · {directory[ticket.createdBy]?.name ?? "Unknown user"}
                          {ticket.assignedAgent && <> → {directory[ticket.assignedAgent]?.name ?? "an agent"}</>}
                        </>
                      )}
                    </p>
                  </td>
                  <td className="px-4 py-3">
                    <PriorityBadge priority={ticket.priority} />
                  </td>
                  <td className="px-4 py-3">
                    <StatusPill status={ticket.status} />
                  </td>
                  <td className="px-4 py-3">
                    <SlaFuse
                      slaState={ticket.slaState}
                      status={ticket.status}
                      dueAt={ticket.dueAt}
                      resolvedAt={ticket.resolvedAt}
                    />
                  </td>
                  <td className="px-4 py-3 font-body text-xs text-slate-500">{formatDate(ticket.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
