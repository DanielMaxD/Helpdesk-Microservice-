import axios, { AxiosError, type AxiosInstance } from "axios";
import type { ApiError } from "@/types/ticket";

const USER_SERVICE_URL = import.meta.env.VITE_USER_SERVICE_URL || "http://localhost:8081";
const TICKET_SERVICE_URL = import.meta.env.VITE_TICKET_SERVICE_URL || "http://localhost:8082";

const TOKEN_STORAGE_KEY = "helpdesk.token";

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearStoredToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

/**
 * One listener the AuthContext registers so a 401 from either backend
 * (expired/invalid token) forces a clean logout instead of leaving the UI in
 * a half-authenticated state.
 */
let onUnauthorized: (() => void) | null = null;
export function registerUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

function createClient(baseURL: string): AxiosInstance {
  const client = axios.create({ baseURL });

  client.interceptors.request.use((config) => {
    const token = getStoredToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    (error: AxiosError<ApiError>) => {
      if (error.response?.status === 401) {
        clearStoredToken();
        onUnauthorized?.();
      }
      return Promise.reject(error);
    }
  );

  return client;
}

export const userServiceClient = createClient(USER_SERVICE_URL);
export const ticketServiceClient = createClient(TICKET_SERVICE_URL);

/**
 * Extracts a human-readable message from the backend's consistent
 * {timestamp, status, error, message, path} error shape, falling back
 * gracefully for network failures or unexpected error shapes.
 */
export function getErrorMessage(error: unknown, fallback = "Something went wrong. Please try again."): string {
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiError | undefined;
    if (apiError?.message) {
      return apiError.message;
    }
    if (error.code === "ERR_NETWORK") {
      return "Could not reach the server. Check that it's running and try again.";
    }
  }
  return fallback;
}
