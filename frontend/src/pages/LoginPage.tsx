import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LifeBuoy } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { getErrorMessage } from "@/api/client";
import FormField from "@/components/FormField";
import ErrorBanner from "@/components/ErrorBanner";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const redirectTo = (location.state as { from?: Location })?.from?.pathname || "/";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await login({ email, password });
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, "Couldn't sign in. Check your email and password."));
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
          <h1 className="font-display text-2xl font-semibold text-white">HelpDesk</h1>
          <p className="font-body text-sm text-slate-400">Sign in to manage your support tickets</p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-2xl bg-paper-0 p-6 shadow-card">
          {error && <ErrorBanner message={error} />}

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
            placeholder="••••••••"
            autoComplete="current-password"
          />

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-2 rounded-lg bg-indigo px-4 py-2.5 font-body text-sm font-semibold text-white transition-colors hover:bg-indigo-600 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "Signing in…" : "Sign in"}
          </button>

          <p className="text-center font-body text-sm text-slate-500">
            New here?{" "}
            <Link to="/register" className="font-medium text-indigo hover:underline">
              Create an account
            </Link>
          </p>
        </form>

        <details className="mt-4 rounded-xl bg-white/5 px-4 py-3 text-xs text-slate-400">
          <summary className="cursor-pointer font-medium text-slate-300">Demo accounts</summary>
          <div className="mt-2 space-y-1 font-mono text-[11px] leading-relaxed">
            <p>admin@helpdesk.dev — ADMIN</p>
            <p>agent1@helpdesk.dev — AGENT</p>
            <p>user1@helpdesk.dev — USER</p>
            <p className="pt-1 text-slate-500">password: Password123!</p>
          </div>
        </details>
      </div>
    </div>
  );
}
