export default function FullScreenSpinner() {
  return (
    <div className="flex h-screen w-full items-center justify-center bg-paper-50">
      <div
        className="h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-indigo"
        role="status"
        aria-label="Loading"
      />
    </div>
  );
}
