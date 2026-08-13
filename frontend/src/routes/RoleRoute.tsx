import { Navigate, Outlet, useOutletContext } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import type { Role } from "@/types/auth";

interface RoleRouteProps {
  allow: Role[];
}

/**
 * A second layer of route gating for role-specific pages (e.g. the admin
 * user directory). This exists purely for navigation/UX - it hides pages
 * the current role shouldn't see. It is never the source of truth for
 * authorization: every backend endpoint enforces its own rules regardless
 * of what the frontend renders or hides.
 *
 * This is rendered one level below AppLayout's <Outlet context={...}>, so it
 * must explicitly re-read that context and forward it onto its own <Outlet>.
 * react-router does not propagate outlet context through an intermediate
 * <Outlet> automatically - each <Outlet> that doesn't forward a context
 * resets it to undefined for whatever renders below it. Without this
 * forwarding, any page routed through RoleRoute (currently just
 * AdminUsersPage, via usePageTitle -> useOutletContext) would throw trying
 * to destructure that undefined context, and - since the app has no error
 * boundary - that throw blanks the entire page, not just this route.
 */
export default function RoleRoute({ allow }: RoleRouteProps) {
  const { user } = useAuth();
  const layoutContext = useOutletContext();

  if (!user || !allow.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet context={layoutContext} />;
}
