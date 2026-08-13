import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { PlusCircle } from "lucide-react";
import { ticketApi } from "@/api/ticketApi";
import { getErrorMessage } from "@/api/client";
import { useAuth } from "@/hooks/useAuth";
import { usePageTitle } from "@/layouts/AppLayout";
import type { TicketStatistics } from "@/types/ticket";
import StatCard from "@/components/StatCard";
import ErrorBanner from "@/components/ErrorBanner";
import FullScreenSpinner from "@/components/FullScreenSpinner";

export default function DashboardPage() {
  usePageTitle("Dashboard");
  const { user } = useAuth();

  const [stats, setStats] = useState<TicketStatistics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    ticketApi
      .getStatistics()
      .then((data) => {
        if (!cancelled) setStats(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, "Couldn't load your ticket statistics."));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const scopeLabel =
    user?.role === "ADMIN" ? "across the whole team" : user?.role === "AGENT" ? "assigned to you" : "you've created";

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="font-display text-xl font-semibold text-ink-950">
            Welcome back, {user?.name.split(" ")[0]}
          </h2>
          <p className="mt-1 font-body text-sm text-slate-500">Here's what's happening with tickets {scopeLabel}.</p>
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

      {error && <ErrorBanner message={error} />}

      {isLoading ? (
        <FullScreenSpinner />
      ) : stats ? (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          <StatCard label="Total" value={stats.totalTickets} />
          <StatCard label="Open" value={stats.openTickets} />
          <StatCard label="In progress" value={stats.inProgressTickets} accent="amber" />
          <StatCard label="Resolved" value={stats.resolvedTickets} accent="teal" />
          <StatCard label="Closed" value={stats.closedTickets} />
          <StatCard label="SLA breached" value={stats.breachedTickets} accent="coral" />
        </div>
      ) : null}

      <div className="rounded-xl border border-dashed border-slate-200 bg-paper-0 p-6 text-center">
        <p className="font-body text-sm text-slate-500">
          Go to{" "}
          <Link to="/tickets" className="font-medium text-indigo hover:underline">
            Tickets
          </Link>{" "}
          to see the full list.
        </p>
      </div>
    </div>
  );
}
