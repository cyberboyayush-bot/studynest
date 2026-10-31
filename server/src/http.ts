import { HTTPException } from "hono/http-exception";
import type { auth } from "./auth";

type SessionResult = NonNullable<Awaited<ReturnType<typeof auth.api.getSession>>>;

export type AppEnv = {
  Variables: {
    user: SessionResult["user"];
    session: SessionResult["session"];
  };
};

export type ErrorCode =
  | "bad_request"
  | "unauthorized"
  | "not_found"
  | "conflict"
  | "payload_too_large"
  | "internal_error";

export function apiError(
  status: 400 | 401 | 404 | 409 | 413 | 500,
  code: ErrorCode,
  message: string,
) {
  return new HTTPException(status, {
    res: Response.json({ error: { code, message } }, { status }),
  });
}

/** Escapes `%`, `_` and `\` so user input is matched literally inside a LIKE pattern. */
export function escapeLike(value: string) {
  return value.replace(/[\\%_]/g, (match) => `\\${match}`);
}

/** Finds a Postgres SQLSTATE code on an error, looking through Drizzle's wrapping `cause`s. */
function pgCode(error: unknown): string | undefined {
  let current: unknown = error;
  for (let depth = 0; depth < 4 && current; depth++) {
    if (typeof current === "object" && current !== null && "code" in current && typeof current.code === "string") {
      if (/^[0-9A-Z]{5}$/.test(current.code)) return current.code;
    }
    current = (current as { cause?: unknown }).cause;
  }
  return undefined;
}

export const isUniqueViolation = (error: unknown) => pgCode(error) === "23505";
export const isForeignKeyViolation = (error: unknown) => pgCode(error) === "23503";
