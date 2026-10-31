import { app } from "../src/app";

type Json = Record<string, unknown>;

export async function call(
  method: string,
  path: string,
  options: { token?: string; body?: unknown; headers?: Record<string, string> } = {},
) {
  const headers: Record<string, string> = { ...options.headers };
  if (options.token) headers.Authorization = `Bearer ${options.token}`;
  if (options.body !== undefined) headers["Content-Type"] = "application/json";

  const res = await app.request(path, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const text = await res.text();
  const json = (text ? JSON.parse(text) : null) as any;
  return { status: res.status, headers: res.headers, json };
}

let counter = 0;

export async function signUp(overrides: Json = {}) {
  counter += 1;
  const credentials = {
    name: "Ayush",
    email: `user${counter}-${crypto.randomUUID().slice(0, 8)}@example.com`,
    password: "correct-horse-battery",
    ...overrides,
  };
  const res = await call("POST", "/api/auth/sign-up/email", { body: credentials });
  const token = res.headers.get("set-auth-token");
  if (res.status !== 200 || !token) {
    throw new Error(`Sign up failed: ${res.status} ${JSON.stringify(res.json)}`);
  }
  return { token, credentials, user: res.json.user };
}

export async function createSubject(token: string, overrides: Json = {}) {
  const res = await call("POST", "/api/subjects", {
    token,
    body: { name: `Subject ${crypto.randomUUID().slice(0, 6)}`, color: "lilac", icon: "book", ...overrides },
  });
  if (res.status !== 201) throw new Error(`Create subject failed: ${res.status} ${JSON.stringify(res.json)}`);
  return res.json.subject as { id: string; name: string };
}

export function assignmentBody(subjectId: string, overrides: Json = {}) {
  return { title: "Problem set 3", subjectId, dueOn: "2026-10-20", priority: "high", notes: "Odd questions", ...overrides };
}

export function sessionBody(overrides: Json = {}) {
  return {
    startedAt: "2026-10-14T09:00:00.000Z",
    durationSeconds: 25 * 60,
    day: "2026-10-14",
    ...overrides,
  };
}
