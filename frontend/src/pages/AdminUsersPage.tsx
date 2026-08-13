import { useEffect, useState } from "react";
import { Trash2 } from "lucide-react";
import { userApi } from "@/api/userApi";
import { getErrorMessage } from "@/api/client";
import { useAuth } from "@/hooks/useAuth";
import { usePageTitle } from "@/layouts/AppLayout";
import type { Role, User } from "@/types/auth";
import ErrorBanner from "@/components/ErrorBanner";
import FullScreenSpinner from "@/components/FullScreenSpinner";
import { formatDate } from "@/utils/format";

const ROLE_OPTIONS: Role[] = ["USER", "AGENT", "ADMIN"];

export default function AdminUsersPage() {
  usePageTitle("Users");
  const { user: currentUser } = useAuth();

  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyUserId, setBusyUserId] = useState<string | null>(null);

  useEffect(() => {
    loadUsers();
  }, []);

  function loadUsers() {
    setIsLoading(true);
    setError(null);
    userApi
      .getAll()
      .then(setUsers)
      .catch((err) => setError(getErrorMessage(err, "Couldn't load users.")))
      .finally(() => setIsLoading(false));
  }

  async function handleRoleChange(id: string, role: Role) {
    setActionError(null);
    setBusyUserId(id);
    try {
      const updated = await userApi.update(id, { role });
      setUsers((prev) => prev.map((u) => (u.id === id ? updated : u)));
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't change that user's role."));
    } finally {
      setBusyUserId(null);
    }
  }

  async function handleToggleActive(id: string, active: boolean) {
    setActionError(null);
    setBusyUserId(id);
    try {
      const updated = await userApi.update(id, { active: !active });
      setUsers((prev) => prev.map((u) => (u.id === id ? updated : u)));
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't update that user's status."));
    } finally {
      setBusyUserId(null);
    }
  }

  async function handleDelete(id: string, name: string) {
    if (!window.confirm(`Delete ${name}? This cannot be undone.`)) return;
    setActionError(null);
    setBusyUserId(id);
    try {
      await userApi.remove(id);
      setUsers((prev) => prev.filter((u) => u.id !== id));
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't delete that user."));
    } finally {
      setBusyUserId(null);
    }
  }

  if (isLoading) return <FullScreenSpinner />;

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h2 className="font-display text-xl font-semibold text-ink-950">Users</h2>
        <p className="mt-1 font-body text-sm text-slate-500">{users.length} accounts</p>
      </div>

      {error && <ErrorBanner message={error} />}
      {actionError && <ErrorBanner message={actionError} />}

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-paper-0 shadow-card">
        <table className="w-full min-w-[720px] text-left">
          <thead>
            <tr className="border-b border-slate-200 bg-paper-50 font-body text-xs uppercase tracking-wide text-slate-500">
              <th className="px-4 py-3 font-medium">Name</th>
              <th className="px-4 py-3 font-medium">Email</th>
              <th className="px-4 py-3 font-medium">Role</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Joined</th>
              <th className="px-4 py-3 font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const isSelf = u.id === currentUser?.id;
              const isBusy = busyUserId === u.id;
              return (
                <tr key={u.id} className="border-b border-slate-200 last:border-0 hover:bg-paper-50">
                  <td className="px-4 py-3 font-body text-sm font-medium text-ink-950">
                    {u.name} {isSelf && <span className="font-normal text-slate-400">(you)</span>}
                  </td>
                  <td className="px-4 py-3 font-body text-sm text-slate-600">{u.email}</td>
                  <td className="px-4 py-3">
                    <select
                      value={u.role}
                      disabled={isSelf || isBusy}
                      onChange={(e) => handleRoleChange(u.id, e.target.value as Role)}
                      className="rounded-md border border-slate-200 bg-paper-0 px-2 py-1 font-body text-xs text-ink-950 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {ROLE_OPTIONS.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleToggleActive(u.id, u.active)}
                      disabled={isSelf || isBusy}
                      className={`rounded-full px-2.5 py-0.5 font-body text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${
                        u.active ? "bg-teal-100 text-teal hover:bg-teal/20" : "bg-coral-100 text-coral hover:bg-coral/20"
                      }`}
                    >
                      {u.active ? "Active" : "Inactive"}
                    </button>
                  </td>
                  <td className="px-4 py-3 font-body text-xs text-slate-500">{formatDate(u.createdAt)}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleDelete(u.id, u.name)}
                      disabled={isSelf || isBusy}
                      title={isSelf ? "You cannot delete your own account" : "Delete user"}
                      className="text-slate-400 transition-colors hover:text-coral disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      <Trash2 size={15} />
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
