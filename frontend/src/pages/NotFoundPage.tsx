import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-paper-50 px-4 text-center">
      <p className="font-mono text-sm text-slate-400">404</p>
      <h1 className="font-display text-2xl font-semibold text-ink-950">Page not found</h1>
      <p className="max-w-sm font-body text-sm text-slate-500">
        This page doesn't exist yet, or the link is out of date.
      </p>
      <Link to="/" className="mt-2 font-body text-sm font-medium text-indigo hover:underline">
        Back to dashboard
      </Link>
    </div>
  );
}
