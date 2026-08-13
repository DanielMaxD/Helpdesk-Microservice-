import { Outlet, useOutletContext } from "react-router-dom";
import { useEffect, useState } from "react";
import Sidebar from "@/components/Sidebar";
import Topbar from "@/components/Topbar";

interface LayoutContext {
  pageTitle: string;
  setPageTitle: (title: string) => void;
}

export default function AppLayout() {
  const [pageTitle, setPageTitle] = useState("Dashboard");
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);

  return (
    <div className="flex h-screen w-full overflow-hidden bg-paper-50">
      <Sidebar isOpen={isMobileNavOpen} onClose={() => setIsMobileNavOpen(false)} />
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Topbar title={pageTitle} onMenuClick={() => setIsMobileNavOpen(true)} />
        <main className="flex-1 overflow-y-auto scrollbar-thin px-4 py-5 sm:px-6 sm:py-6">
          <Outlet context={{ pageTitle, setPageTitle } satisfies LayoutContext} />
        </main>
      </div>
    </div>
  );
}

export function usePageTitle(title: string) {
  const { setPageTitle } = useOutletContext<LayoutContext>();

  useEffect(() => {
    setPageTitle(title);
  }, [title, setPageTitle]);
}
