import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ticketApi } from "@/api/ticketApi";
import { getErrorMessage } from "@/api/client";
import { usePageTitle } from "@/layouts/AppLayout";
import type { Category, Priority } from "@/types/ticket";
import FormField from "@/components/FormField";
import SelectField from "@/components/SelectField";
import ErrorBanner from "@/components/ErrorBanner";

const PRIORITY_OPTIONS: Array<{ value: Priority; label: string }> = [
  { value: "LOW", label: "Low — response within 24h" },
  { value: "MEDIUM", label: "Medium — response within 8h" },
  { value: "HIGH", label: "High — response within 4h" },
  { value: "CRITICAL", label: "Critical — response within 1h" },
];

const CATEGORY_OPTIONS: Array<{ value: Category; label: string }> = [
  { value: "ACCOUNT", label: "Account" },
  { value: "PAYMENT", label: "Payment" },
  { value: "TECHNICAL", label: "Technical" },
  { value: "BILLING", label: "Billing" },
  { value: "OTHER", label: "Other" },
];

export default function NewTicketPage() {
  usePageTitle("New ticket");
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [category, setCategory] = useState<Category>("OTHER");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const ticket = await ticketApi.create({ title, description, priority, category });
      navigate(`/tickets/${ticket.id}`);
    } catch (err) {
      setError(getErrorMessage(err, "Couldn't create the ticket. Check the fields and try again."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-5">
      <div>
        <h2 className="font-display text-xl font-semibold text-ink-950">Describe your issue</h2>
        <p className="mt-1 font-body text-sm text-slate-500">
          Give us enough detail to help quickly — what happened, when, and what you expected instead.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-paper-0 p-6 shadow-card">
        {error && <ErrorBanner message={error} />}

        <FormField
          label="Title"
          id="title"
          value={title}
          onChange={setTitle}
          placeholder="Short summary of the issue"
        />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="description" className="font-body text-sm font-medium text-ink-950">
            Description
          </label>
          <textarea
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            rows={6}
            placeholder="What were you trying to do? What happened instead?"
            className="resize-none rounded-lg border border-slate-200 bg-paper-0 px-3.5 py-2.5 font-body text-sm text-ink-950 placeholder:text-slate-400 transition-colors focus:border-indigo focus:outline-none focus:ring-2 focus:ring-indigo-100"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <SelectField
            label="Priority"
            id="priority"
            value={priority}
            onChange={(v) => setPriority(v as Priority)}
            options={PRIORITY_OPTIONS}
          />
          <SelectField
            label="Category"
            id="category"
            value={category}
            onChange={(v) => setCategory(v as Category)}
            options={CATEGORY_OPTIONS}
          />
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="mt-2 self-start rounded-lg bg-indigo px-5 py-2.5 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting ? "Submitting…" : "Submit ticket"}
        </button>
      </form>
    </div>
  );
}
