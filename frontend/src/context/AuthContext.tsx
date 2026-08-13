import React, { createContext, useCallback, useEffect, useMemo, useState } from "react";
import { authApi } from "@/api/authApi";
import {
  clearStoredToken,
  getStoredToken,
  registerUnauthorizedHandler,
  setStoredToken,
} from "@/api/client";
import { userApi } from "@/api/userApi";
import type { LoginRequest, RegisterRequest, User } from "@/types/auth";

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const logout = useCallback(() => {
    clearStoredToken();
    setUser(null);
  }, []);

  // On first load, if a token is already stored, restore the session by
  // decoding it just enough to know the user id, then fetching the full
  // profile - this keeps the frontend from trusting anything except what
  // user-service itself returns.
  useEffect(() => {
    registerUnauthorizedHandler(logout);

    const token = getStoredToken();
    if (!token) {
      setIsLoading(false);
      return;
    }

    const userId = decodeUserIdFromToken(token);
    if (!userId) {
      clearStoredToken();
      setIsLoading(false);
      return;
    }

    userApi
      .getById(userId)
      .then(setUser)
      .catch(() => clearStoredToken())
      .finally(() => setIsLoading(false));
  }, [logout]);

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authApi.login(request);
    setStoredToken(response.token);
    setUser(response.user);
  }, []);

  const register = useCallback(async (request: RegisterRequest) => {
    const response = await authApi.register(request);
    setStoredToken(response.token);
    setUser(response.user);
  }, []);

  const value = useMemo(
    () => ({ user, isLoading, login, register, logout }),
    [user, isLoading, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Reads only the "sub" (user id) claim out of the JWT payload, client-side,
 * purely to know which profile to fetch on page refresh. This is NOT used
 * for any authorization decision - the backend is always the source of
 * truth for role and permissions on every request.
 */
function decodeUserIdFromToken(token: string): string | null {
  try {
    const payloadBase64 = token.split(".")[1];
    const payloadJson = atob(payloadBase64.replace(/-/g, "+").replace(/_/g, "/"));
    const payload = JSON.parse(payloadJson) as { sub?: string };
    return payload.sub ?? null;
  } catch {
    return null;
  }
}
