import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Bell, BellOff, CheckCheck, UserPlus, RefreshCw, AlertTriangle } from "lucide-react";
import { notificationApi } from "@/api/notificationApi";
import { getErrorMessage } from "@/api/client";
import { usePageTitle } from "@/layouts/AppLayout";
import type { Notification, NotificationType } from "@/types/ticket";
import ErrorBanner from "@/components/ErrorBanner";
import EmptyState from "@/components/EmptyState";
import FullScreenSpinner from "@/components/FullScreenSpinner";
import { timeAgo } from "@/utils/format";

const TYPE_ICON: Record<NotificationType, typeof Bell> = {
  TICKET_ASSIGNED: UserPlus,
  TICKET_UPDATED: RefreshCw,
  TICKET_RESOLVED: CheckCheck,
  SLA_WARNING: AlertTriangle,
};

const TYPE_STYLE: Record<NotificationType, string> = {
  TICKET_ASSIGNED: "bg-indigo-100 text-indigo-600",
  TICKET_UPDATED: "bg-slate-200/60 text-slate-600",
  TICKET_RESOLVED: "bg-teal-100 text-teal",
  SLA_WARNING: "bg-amber-100 text-amber-DEFAULT",
};

export default function NotificationsPage() {
  usePageTitle("Notifications");

  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setIsLoading(true);
    setError(null);
    notificationApi
      .getAll()
      .then(setNotifications)
      .catch((err) => setError(getErrorMessage(err, "Couldn't load notifications.")))
      .finally(() => setIsLoading(false));
  }

  async function handleMarkAsRead(id: string) {
    try {
      const updated = await notificationApi.markAsRead(id);
      setNotifications((prev) => prev.map((n) => (n.id === id ? updated : n)));
    } catch {
      // non-fatal - the item just stays unread visually, no need to interrupt the user
    }
  }

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-5">
      <div>
        <h2 className="font-display text-xl font-semibold text-ink-950">Notifications</h2>
        <p className="mt-1 font-body text-sm text-slate-500">
          {unreadCount > 0 ? `${unreadCount} unread` : "You're all caught up"}
        </p>
      </div>

      {error && <ErrorBanner message={error} />}

      {isLoading ? (
        <FullScreenSpinner />
      ) : notifications.length === 0 ? (
        <EmptyState icon={BellOff} title="No notifications yet" description="Ticket updates will show up here." />
      ) : (
        <div className="flex flex-col gap-2">
          {notifications.map((n) => {
            const Icon = TYPE_ICON[n.type];
            return (
              <div
                key={n.id}
                className={`flex items-start gap-3 rounded-xl border px-4 py-3.5 transition-colors ${
                  n.read ? "border-slate-200 bg-paper-0" : "border-indigo-100 bg-indigo-100/30"
                }`}
              >
                <div className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${TYPE_STYLE[n.type]}`}>
                  <Icon size={15} />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-body text-sm text-ink-950">{n.message}</p>
                  <p className="mt-1 font-mono text-[11px] text-slate-400">{timeAgo(n.createdAt)}</p>
                </div>
                {!n.read && (
                  <button
                    onClick={() => handleMarkAsRead(n.id)}
                    className="shrink-0 rounded-md px-2 py-1 font-body text-xs font-medium text-indigo hover:bg-indigo-100"
                  >
                    Mark read
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      <p className="text-center font-body text-xs text-slate-400">
        Go to <Link to="/tickets" className="text-indigo hover:underline">Tickets</Link> to see what's changed.
      </p>
    </div>
  );
}
