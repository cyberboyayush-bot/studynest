import { describe, expect, test } from "bun:test";
import { call, signUp } from "./helpers";

describe("auth", () => {
  test("sign up returns a signed bearer token and the user", async () => {
    const { token, user, credentials } = await signUp();
    expect(token).toContain(".");
    expect(user.email).toBe(credentials.email);
    expect(user.dailyFocusGoalMinutes).toBe(60);
    expect(user).not.toHaveProperty("password");
  });

  test("sign up rejects a duplicate email", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-up/email", { body: credentials });
    expect(res.status).toBe(422);
  });

  test("sign up treats email case-insensitively", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: { ...credentials, email: credentials.email.toUpperCase() },
    });
    expect(res.status).toBe(422);
  });

  test("sign up rejects short passwords", async () => {
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: { name: "A", email: "short@example.com", password: "1234567" },
    });
    expect(res.status).toBe(400);
  });

  test("sign up rejects invalid email", async () => {
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: { name: "A", email: "not-an-email", password: "long-enough-pass" },
    });
    expect(res.status).toBe(400);
  });

  test.each([
    ["blank", "   "],
    ["too long", "x".repeat(61)],
  ])("sign up rejects a %s name", async (_, name) => {
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: { name, email: `name-${crypto.randomUUID()}@example.com`, password: "long-enough-pass" },
    });
    expect(res.status).toBe(400);
  });

  test.each([
    ["blank", ""],
    ["too long", "x".repeat(61)],
    ["not a string", 42],
  ])("update-user rejects a %s name", async (_, name) => {
    const { token } = await signUp();
    const res = await call("POST", "/api/auth/update-user", { token, body: { name } });
    expect(res.status).toBe(400);
    expect((await call("GET", "/api/me", { token })).json.profile.name).toBe("Ayush");
  });

  test("update-user trims a valid name", async () => {
    const { token } = await signUp();
    const res = await call("POST", "/api/auth/update-user", { token, body: { name: "  Maya  " } });
    expect(res.status).toBe(200);
    expect((await call("GET", "/api/me", { token })).json.profile.name).toBe("Maya");
  });

  test("sign up trims the name", async () => {
    const { user } = await signUp({ name: "  Ayush  " });
    expect(user.name).toBe("Ayush");
  });

  test("forged client IP headers from callers are discarded", async () => {
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: "nobody@example.com", password: "whatever-password" },
      headers: { "x-studynest-client-ip": "1.2.3.4", "x-forwarded-for": "5.6.7.8" },
    });
    expect(res.status).toBe(401);
  });

  test("clients cannot set dailyFocusGoalMinutes during sign up", async () => {
    const res = await call("POST", "/api/auth/sign-up/email", {
      body: {
        name: "Sneaky",
        email: "sneaky@example.com",
        password: "long-enough-pass",
        dailyFocusGoalMinutes: 999,
      },
    });
    if (res.status === 200) expect(res.json.user.dailyFocusGoalMinutes).toBe(60);
    else expect(res.status).toBe(400);
  });

  test("sign in is case-insensitive for the email", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: credentials.email.toUpperCase(), password: credentials.password },
    });
    expect(res.status).toBe(200);
  });

  test("sign in works with correct credentials", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: credentials.email, password: credentials.password },
    });
    expect(res.status).toBe(200);
    expect(res.headers.get("set-auth-token")).toBeTruthy();
  });

  test("sign in fails with a wrong password", async () => {
    const { credentials } = await signUp();
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: credentials.email, password: "wrong-password" },
    });
    expect(res.status).toBe(401);
    expect(res.headers.get("set-auth-token")).toBeNull();
  });

  test("sign in fails for an unknown user", async () => {
    const res = await call("POST", "/api/auth/sign-in/email", {
      body: { email: "nobody@example.com", password: "whatever-password" },
    });
    expect(res.status).toBe(401);
  });

  test("get-session works with the bearer token", async () => {
    const { token, credentials } = await signUp();
    const res = await call("GET", "/api/auth/get-session", { token });
    expect(res.status).toBe(200);
    expect(res.json.user.email).toBe(credentials.email);
  });

  test("an unsigned raw token is rejected", async () => {
    const { token } = await signUp();
    const raw = token.split(".")[0]!;
    const res = await call("GET", "/api/me", { token: raw });
    expect(res.status).toBe(401);
  });

  test("a tampered token is rejected", async () => {
    const { token } = await signUp();
    const [raw, signature] = token.split(".");
    const res = await call("GET", "/api/me", { token: `${raw}x.${signature}` });
    expect(res.status).toBe(401);
  });

  test("sign out revokes the session", async () => {
    const { token } = await signUp();
    expect((await call("GET", "/api/me", { token })).status).toBe(200);
    const out = await call("POST", "/api/auth/sign-out", { token, body: {} });
    expect(out.status).toBe(200);
    expect((await call("GET", "/api/me", { token })).status).toBe(401);
  });
});

describe("protected routes", () => {
  test.each([
    ["GET", "/api/me"],
    ["PATCH", "/api/me"],
    ["GET", "/api/subjects"],
    ["POST", "/api/subjects"],
    ["GET", "/api/subjects/00000000-0000-4000-8000-000000000000"],
    ["PATCH", "/api/subjects/00000000-0000-4000-8000-000000000000"],
    ["DELETE", "/api/subjects/00000000-0000-4000-8000-000000000000"],
    ["GET", "/api/assignments"],
    ["POST", "/api/assignments"],
    ["GET", "/api/assignments/00000000-0000-4000-8000-000000000000"],
    ["PATCH", "/api/assignments/00000000-0000-4000-8000-000000000000"],
    ["DELETE", "/api/assignments/00000000-0000-4000-8000-000000000000"],
    ["GET", "/api/focus-sessions?week=2026-10-12"],
    ["POST", "/api/focus-sessions"],
    ["GET", "/api/insights?week=2026-10-12"],
  ])("%s %s requires a session", async (method, path) => {
    const res = await call(method, path);
    expect(res.status).toBe(401);
    expect(res.json.error.code).toBe("unauthorized");
  });

  test("garbage bearer tokens are rejected", async () => {
    const res = await call("GET", "/api/me", { token: "abc.def" });
    expect(res.status).toBe(401);
  });
});

describe("misc", () => {
  test("health check", async () => {
    const res = await call("GET", "/health");
    expect(res.json).toEqual({ ok: true });
  });

  test("unknown routes return a JSON 404", async () => {
    const { token } = await signUp();
    const res = await call("GET", "/api/nope", { token });
    expect(res.status).toBe(404);
    expect(res.json.error.code).toBe("not_found");
  });

  test("security headers are present", async () => {
    const res = await call("GET", "/health");
    expect(res.headers.get("x-content-type-options")).toBe("nosniff");
    expect(res.headers.get("x-frame-options")).toBeTruthy();
  });

  test("oversized bodies are rejected", async () => {
    const { token } = await signUp();
    const res = await call("POST", "/api/subjects", {
      token,
      body: { name: "x".repeat(20_000), color: "lilac", icon: "book" },
    });
    expect(res.status).toBe(413);
  });
});
