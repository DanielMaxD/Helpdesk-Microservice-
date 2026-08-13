import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LifeBuoy } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { getErrorMessage } from "@/api/client";
import FormField from "@/components/FormField";
import ErrorBanner from "@/components/ErrorBanner";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await register({ name, email, password });
      navigate("/", { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, "Couldn't create your account. Please check the details and try again."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-ink-950 px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10">
            <LifeBuoy size={22} className="text-indigo-100" strokeWidth={2.25} />
          </div>
          <h1 className="font-display text-2xl font-semibold text-white">Create your account</h1>
          <p className="font-body text-sm text-slate-400">New accounts start as a standard user</p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-2xl bg-paper-0 p-6 shadow-card">
          {error && <ErrorBanner message={error} />}

          <FormField label="Full name" id="name" value={name} onChange={setName} placeholder="Jane Doe" autoComplete="name" />
          <FormField
            label="Email"
            id="email"
            type="email"
            value={email}
            onChange={setEmail}
            placeholder="you@company.com"
            autoComplete="email"
          />
          <FormField
            label="Password"
            id="password"
            type="password"
            value={password}
            onChange={setPassword}
            placeholder="At least 6 characters"
            autoComplete="new-password"
          />

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-2 rounded-lg bg-indigo px-4 py-2.5 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "Creating account…" : "Create account"}
          </button>

          <p className="text-center font-body text-sm text-slate-500">
            Already have an account?{" "}
            <Link to="/login" className="font-medium text-indigo hover:underline">
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
