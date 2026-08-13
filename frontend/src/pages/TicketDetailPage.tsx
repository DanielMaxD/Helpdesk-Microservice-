import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowRight, Pencil, Trash2, UserPlus, X } from "lucide-react";
import { ticketApi } from "@/api/ticketApi";
import { userApi } from "@/api/userApi";
import { getErrorMessage } from "@/api/client";
import { useAuth } from "@/hooks/useAuth";
import { useUserDirectory } from "@/hooks/useUserDirectory";
import { usePageTitle } from "@/layouts/AppLayout";
import type { Category, Comment, Priority, Status, Ticket } from "@/types/ticket";
import type { User } from "@/types/auth";
import PriorityBadge from "@/components/PriorityBadge";
import StatusPill from "@/components/StatusPill";
import StatusStepper from "@/components/StatusStepper";
import SlaFuse from "@/components/SlaFuse";
import ErrorBanner from "@/components/ErrorBanner";
import FullScreenSpinner from "@/components/FullScreenSpinner";
import SelectField from "@/components/SelectField";
import { formatDateTime, timeAgo, initials } from "@/utils/format";

const NEXT_STATUS: Record<Status, Status | null> = {
  OPEN: "IN_PROGRESS",
  IN_PROGRESS: "RESOLVED",
  RESOLVED: "CLOSED",
  CLOSED: null,
};

const NEXT_STATUS_LABEL: Record<Status, string> = {
  OPEN: "Start working on this ticket",
  IN_PROGRESS: "Mark as resolved",
  RESOLVED: "Close ticket",
  CLOSED: "",
};

const PRIORITY_OPTIONS = [
  { value: "LOW", label: "Low" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HIGH", label: "High" },
  { value: "CRITICAL", label: "Critical" },
];

const CATEGORY_OPTIONS = [
  { value: "ACCOUNT", label: "Account" },
  { value: "PAYMENT", label: "Payment" },
  { value: "TECHNICAL", label: "Technical" },
  { value: "BILLING", label: "Billing" },
  { value: "OTHER", label: "Other" },
];

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const directory = useUserDirectory();

  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [agents, setAgents] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isActing, setIsActing] = useState(false);

  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editPriority, setEditPriority] = useState<Priority>("MEDIUM");
  const [editCategory, setEditCategory] = useState<Category>("OTHER");

  const [commentDraft, setCommentDraft] = useState("");
  const [selectedAgentId, setSelectedAgentId] = useState("");

  usePageTitle(ticket ? ticket.title : "Ticket");

  useEffect(() => {
    if (!id) return;
    loadTicket();
    ticketApi
      .getComments(id)
      .then(setComments)
      .catch(() => {});
    if (user?.role === "ADMIN") {
      userApi
        .getAgents()
        .then(setAgents)
        .catch(() => {});
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user?.role]);

  function loadTicket() {
    if (!id) return;
    setIsLoading(true);
    setError(null);
    ticketApi
      .getById(id)
      .then((data) => {
        setTicket(data);
        setEditTitle(data.title);
        setEditDescription(data.description);
        setEditPriority(data.priority);
        setEditCategory(data.category);
      })
      .catch((err) => setError(getErrorMessage(err, "Couldn't load this ticket.")))
      .finally(() => setIsLoading(false));
  }

  if (isLoading) return <FullScreenSpinner />;
  if (error) return <ErrorBanner message={error} />;
  if (!ticket || !user) return null;

  const isOwner = ticket.createdBy === user.id;
  const isAssignedAgent = user.role === "AGENT" && ticket.assignedAgent === user.id;
  const isAdmin = user.role === "ADMIN";
  const isClosed = ticket.status === "CLOSED";

  const canEdit = (isAdmin || isAssignedAgent) && !isClosed;
  const canComment = (isAdmin || isAssignedAgent || (user.role === "USER" && isOwner)) && !isClosed;
  const canAssign = isAdmin && !isClosed;
  const canDelete = isAdmin;

  const nextStatus = NEXT_STATUS[ticket.status];
  const canAdvance =
    nextStatus !== null &&
    (isAdmin || isAssignedAgent || (user.role === "USER" && isOwner && ticket.status === "RESOLVED"));

  async function handleAdvanceStatus() {
    if (!id || !nextStatus) return;
    setActionError(null);
    setIsActing(true);
    try {
      const updated = await ticketApi.updateStatus(id, { status: nextStatus });
      setTicket(updated);
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't update the status."));
    } finally {
      setIsActing(false);
    }
  }

  async function handleSaveEdit(e: React.FormEvent) {
    e.preventDefault();
    if (!id) return;
    setActionError(null);
    setIsActing(true);
    try {
      const updated = await ticketApi.update(id, {
        title: editTitle,
        description: editDescription,
        priority: editPriority,
        category: editCategory,
      });
      setTicket(updated);
      setIsEditing(false);
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't save changes."));
    } finally {
      setIsActing(false);
    }
  }

  async function handleAssign() {
    if (!id || !selectedAgentId) return;
    setActionError(null);
    setIsActing(true);
    try {
      const updated = await ticketApi.assign(id, { agentId: selectedAgentId });
      setTicket(updated);
      setSelectedAgentId("");
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't assign this ticket."));
    } finally {
      setIsActing(false);
    }
  }

  async function handleAddComment(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !commentDraft.trim()) return;
    setActionError(null);
    setIsActing(true);
    try {
      const comment = await ticketApi.addComment(id, { message: commentDraft.trim() });
      setComments((prev) => [...prev, comment]);
      setCommentDraft("");
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't post your comment."));
    } finally {
      setIsActing(false);
    }
  }

  async function handleDelete() {
    if (!id) return;
    if (!window.confirm("Delete this ticket permanently? This cannot be undone.")) return;
    setIsActing(true);
    try {
      await ticketApi.remove(id);
      navigate("/tickets");
    } catch (err) {
      setActionError(getErrorMessage(err, "Couldn't delete this ticket."));
      setIsActing(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      {/* Header */}
      <div className="rounded-xl border border-slate-200 bg-paper-0 p-6 shadow-card">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0">
            <p className="font-mono text-xs text-slate-400">#{ticket.id.slice(0, 8)}</p>
            <h2 className="mt-1 break-words font-display text-xl font-semibold text-ink-950">{ticket.title}</h2>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <PriorityBadge priority={ticket.priority} />
              <StatusPill status={ticket.status} />
              <span className="font-body text-xs text-slate-400">
                {ticket.category.charAt(0) + ticket.category.slice(1).toLowerCase()}
              </span>
            </div>
          </div>
          <div className="flex shrink-0 gap-2">
            {canEdit && !isEditing && (
              <button
                onClick={() => setIsEditing(true)}
                className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 font-body text-xs font-medium text-slate-600 hover:bg-paper-50"
              >
                <Pencil size={13} />
                Edit
              </button>
            )}
            {canDelete && (
              <button
                onClick={handleDelete}
                disabled={isActing}
                className="flex items-center gap-1.5 rounded-lg border border-coral/30 px-3 py-1.5 font-body text-xs font-medium text-coral hover:bg-coral-100 disabled:opacity-50"
              >
                <Trash2 size={13} />
                Delete
              </button>
            )}
          </div>
        </div>

        <div className="mt-5 flex justify-center overflow-x-auto py-2">
          <StatusStepper current={ticket.status} />
        </div>

        <div className="mt-4">
          <SlaFuse
            slaState={ticket.slaState}
            status={ticket.status}
            dueAt={ticket.dueAt}
            resolvedAt={ticket.resolvedAt}
            variant="detail"
          />
        </div>

        {actionError && (
          <div className="mt-4">
            <ErrorBanner message={actionError} />
          </div>
        )}

        {canAdvance && nextStatus && (
          <button
            onClick={handleAdvanceStatus}
            disabled={isActing}
            className="mt-4 flex items-center gap-2 rounded-lg bg-indigo px-4 py-2 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {NEXT_STATUS_LABEL[ticket.status]}
            <ArrowRight size={15} />
          </button>
        )}

        <dl className="mt-5 grid grid-cols-2 gap-x-6 gap-y-2 border-t border-slate-200 pt-4 font-body text-xs sm:grid-cols-4">
          <div>
            <dt className="text-slate-400">Created</dt>
            <dd className="mt-0.5 text-slate-600">{formatDateTime(ticket.createdAt)}</dd>
          </div>
          <div>
            <dt className="text-slate-400">Due</dt>
            <dd className="mt-0.5 text-slate-600">{formatDateTime(ticket.dueAt)}</dd>
          </div>
          <div>
            <dt className="text-slate-400">Reported by</dt>
            <dd className="mt-0.5 text-slate-600">
              {ticket.createdBy === user.id ? "You" : directory[ticket.createdBy]?.name ?? "—"}
            </dd>
          </div>
          <div>
            <dt className="text-slate-400">Assigned to</dt>
            <dd className="mt-0.5 text-slate-600">
              {!ticket.assignedAgent
                ? "Unassigned"
                : ticket.assignedAgent === user.id
                ? "You"
                : directory[ticket.assignedAgent]?.name ?? "An agent"}
            </dd>
          </div>
        </dl>
      </div>

      {/* Description / edit form */}
      <div className="rounded-xl border border-slate-200 bg-paper-0 p-6 shadow-card">
        {isEditing ? (
          <form onSubmit={handleSaveEdit} className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <h3 className="font-display text-base font-semibold text-ink-950">Edit ticket</h3>
              <button type="button" onClick={() => setIsEditing(false)} className="text-slate-400 hover:text-slate-600">
                <X size={18} />
              </button>
            </div>
            <input
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              required
              className="rounded-lg border border-slate-200 bg-paper-0 px-3.5 py-2.5 font-body text-sm text-ink-950 focus:border-indigo focus:outline-none focus:ring-2 focus:ring-indigo-100"
            />
            <textarea
              value={editDescription}
              onChange={(e) => setEditDescription(e.target.value)}
              required
              rows={5}
              className="resize-none rounded-lg border border-slate-200 bg-paper-0 px-3.5 py-2.5 font-body text-sm text-ink-950 focus:border-indigo focus:outline-none focus:ring-2 focus:ring-indigo-100"
            />
            <div className="grid grid-cols-2 gap-4">
              <SelectField
                label="Priority"
                id="edit-priority"
                value={editPriority}
                onChange={(v) => setEditPriority(v as Priority)}
                options={PRIORITY_OPTIONS}
              />
              <SelectField
                label="Category"
                id="edit-category"
                value={editCategory}
                onChange={(v) => setEditCategory(v as Category)}
                options={CATEGORY_OPTIONS}
              />
            </div>
            <button
              type="submit"
              disabled={isActing}
              className="self-start rounded-lg bg-indigo px-4 py-2 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600 disabled:opacity-60"
            >
              Save changes
            </button>
          </form>
        ) : (
          <>
            <h3 className="font-display text-base font-semibold text-ink-950">Description</h3>
            <p className="mt-2 whitespace-pre-wrap font-body text-sm leading-relaxed text-slate-600">
              {ticket.description}
            </p>
          </>
        )}
      </div>

      {/* Assignment (admin only) */}
      {canAssign && (
        <div className="rounded-xl border border-slate-200 bg-paper-0 p-6 shadow-card">
          <h3 className="font-display text-base font-semibold text-ink-950">Assignment</h3>
          <div className="mt-3 flex flex-wrap items-end gap-3">
            <div className="min-w-[220px] flex-1">
              <SelectField
                label="Assign to agent"
                id="assign-agent"
                value={selectedAgentId}
                onChange={setSelectedAgentId}
                options={[
                  { value: "", label: ticket.assignedAgent ? "Reassign to…" : "Choose an agent…" },
                  ...agents.map((a) => ({ value: a.id, label: `${a.name}${a.active ? "" : " (inactive)"}` })),
                ]}
                required={false}
              />
            </div>
            <button
              onClick={handleAssign}
              disabled={!selectedAgentId || isActing}
              className="flex items-center gap-2 rounded-lg bg-indigo px-4 py-2.5 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <UserPlus size={15} />
              Assign
            </button>
          </div>
        </div>
      )}

      {/* Comments */}
      <div className="rounded-xl border border-slate-200 bg-paper-0 p-6 shadow-card">
        <h3 className="font-display text-base font-semibold text-ink-950">
          Comments {comments.length > 0 && <span className="text-slate-400">({comments.length})</span>}
        </h3>

        <div className="mt-4 flex flex-col gap-4">
          {comments.length === 0 && <p className="font-body text-sm text-slate-400">No comments yet.</p>}
          {comments.map((comment) => {
            const author = directory[comment.userId];
            const isYou = comment.userId === user.id;
            return (
              <div key={comment.id} className="flex gap-3">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-indigo-100 font-mono text-xs font-medium text-indigo-600">
                  {isYou ? initials(user.name) : author ? initials(author.name) : "?"}
                </div>
                <div className="min-w-0 flex-1 rounded-lg bg-paper-50 px-3.5 py-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-body text-xs font-medium text-ink-950">
                      {isYou ? "You" : author?.name ?? "Someone"}
                    </span>
                    <span className="font-mono text-[11px] text-slate-400">{timeAgo(comment.createdAt)}</span>
                  </div>
                  <p className="mt-1 whitespace-pre-wrap font-body text-sm text-slate-600">{comment.message}</p>
                </div>
              </div>
            );
          })}
        </div>

        {canComment ? (
          <form onSubmit={handleAddComment} className="mt-5 flex flex-col gap-2 border-t border-slate-200 pt-4">
            <textarea
              value={commentDraft}
              onChange={(e) => setCommentDraft(e.target.value)}
              rows={3}
              placeholder="Add a comment…"
              className="resize-none rounded-lg border border-slate-200 bg-paper-0 px-3.5 py-2.5 font-body text-sm text-ink-950 placeholder:text-slate-400 focus:border-indigo focus:outline-none focus:ring-2 focus:ring-indigo-100"
            />
            <button
              type="submit"
              disabled={isActing || !commentDraft.trim()}
              className="self-start rounded-lg bg-indigo px-4 py-2 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Post comment
            </button>
          </form>
        ) : (
          isClosed && (
            <p className="mt-4 border-t border-slate-200 pt-4 font-body text-xs text-slate-400">
              This ticket is closed and can no longer receive comments.
            </p>
          )
        )}
      </div>
    </div>
  );
}
