import { useEffect, useState } from "react";
import { userApi } from "@/api/userApi";
import { useAuth } from "@/hooks/useAuth";
import type { User } from "@/types/auth";

/**
 * Only ADMIN can call GET /api/users (user-service restricts it there, not
 * just here) - so this hook only ever fetches for admins, and quietly
 * returns an empty lookup for everyone else. Components using this should
 * fall back to a generic label ("Assigned", "You") rather than a raw UUID
 * when a name isn't available, since a USER or AGENT genuinely has no way
 * to resolve an arbitrary user's identity - that's an intentional backend
 * boundary, not a bug.
 */
export function useUserDirectory() {
  const { user } = useAuth();
  const [byId, setById] = useState<Record<string, User>>({});

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    let cancelled = false;
    userApi
      .getAll()
      .then((users) => {
        if (!cancelled) {
          setById(Object.fromEntries(users.map((u) => [u.id, u])));
        }
      })
      .catch(() => {
        /* non-fatal - directory just stays empty and callers fall back */
      });
    return () => {
      cancelled = true;
    };
  }, [user?.role]);

  return byId;
}
