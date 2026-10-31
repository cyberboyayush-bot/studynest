import { describe, expect, test } from "bun:test";
import { addDays, streaks } from "../src/dates";
import { assignmentBody, call, createSubject, sessionBody, signUp } from "./helpers";

const WEEK = "2026-10-12"; // a Monday

describe("focus sessions", () => {
  test("records a session and lists it in its week", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token, { name: "Physics", color: "mint", icon: "flask" });

    const created = await call("POST", "/api/focus-sessions", {
      token,
      body: sessionBody({ subjectId: subject.id }),
    });
    expect(created.status).toBe(201);
    expect(created.json.session).toMatchObject({
      durationSeconds: 1500,
      day: "2026-10-14",
      startedAt: "2026-10-14T09:00:00.000Z",
      subject: { id: subject.id, name: "Physics" },
    });

    const list = await call("GET", `/api/focus-sessions?week=${WEEK}`, { token });
    expect(list.json.sessions).toHaveLength(1);
    const otherWeek = await call("GET", "/api/focus-sessions?week=2026-10-19", { token });
    expect(otherWeek.json.sessions).toHaveLength(0);
  });

  test("retrying with the same client id records the session once", async () => {
    const { token } = await signUp();
    const id = crypto.randomUUID();
    const first = await call("POST", "/api/focus-sessions", { token, body: sessionBody({ id }) });
    expect(first.status).toBe(201);
    expect(first.json.session.id).toBe(id);
    const retry = await call("POST", "/api/focus-sessions", { token, body: sessionBody({ id }) });
    expect(retry.status).toBe(200);
    expect(retry.json.session.id).toBe(id);
    const list = await call("GET", `/api/focus-sessions?week=${WEEK}`, { token });
    expect(list.json.sessions).toHaveLength(1);
  });

  test("another user's session id can't be reused or read", async () => {
    const alice = await signUp();
    const bob = await signUp();
    const id = crypto.randomUUID();
    await call("POST", "/api/focus-sessions", { token: alice.token, body: sessionBody({ id }) });
    const res = await call("POST", "/api/focus-sessions", { token: bob.token, body: sessionBody({ id }) });
    expect(res.status).toBe(409);
    expect(res.json.session).toBeUndefined();
  });

  test("a session without a subject is allowed", async () => {
    const { token } = await signUp();
    const res = await call("POST", "/api/focus-sessions", { token, body: sessionBody({ subjectId: null }) });
    expect(res.status).toBe(201);
    expect(res.json.session.subject).toBeNull();
  });

  test("deleting a subject keeps its sessions without a subject", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token);
    await call("POST", "/api/focus-sessions", { token, body: sessionBody({ subjectId: subject.id }) });
    await call("DELETE", `/api/subjects/${subject.id}`, { token });
    const list = await call("GET", `/api/focus-sessions?week=${WEEK}`, { token });
    expect(list.json.sessions).toHaveLength(1);
    expect(list.json.sessions[0].subject).toBeNull();
  });

  test.each([
    ["under a minute", { durationSeconds: 59 }],
    ["over four hours", { durationSeconds: 4 * 3600 + 1 }],
    ["fractional seconds", { durationSeconds: 90.5 }],
    ["bad timestamp", { startedAt: "yesterday" }],
    ["timestamp without offset", { startedAt: "2026-10-14T09:00:00" }],
    ["bad day", { day: "2026-13-01" }],
    ["unknown subject", { subjectId: "00000000-0000-4000-8000-000000000000" }],
    ["unknown field", { userId: "x" }],
    ["future start", { startedAt: new Date(Date.now() + 3_600_000).toISOString() }],
  ])("rejects %s", async (_, override) => {
    const { token } = await signUp();
    const res = await call("POST", "/api/focus-sessions", { token, body: sessionBody(override) });
    expect(res.status).toBe(400);
  });

  test("week must be a Monday", async () => {
    const { token } = await signUp();
    expect((await call("GET", "/api/focus-sessions?week=2026-10-13", { token })).status).toBe(400);
    expect((await call("GET", "/api/focus-sessions", { token })).status).toBe(400);
    expect((await call("GET", "/api/insights?week=2026-10-18", { token })).status).toBe(400);
  });

  test("sessions are private to their owner", async () => {
    const alice = await signUp();
    const bob = await signUp();
    await call("POST", "/api/focus-sessions", { token: alice.token, body: sessionBody() });
    const list = await call("GET", `/api/focus-sessions?week=${WEEK}`, { token: bob.token });
    expect(list.json.sessions).toEqual([]);
    const insights = await call("GET", `/api/insights?week=${WEEK}`, { token: bob.token });
    expect(insights.json.totalSeconds).toBe(0);
  });
});

describe("insights", () => {
  test("an empty week returns zeroed days", async () => {
    const { token } = await signUp();
    const res = await call("GET", `/api/insights?week=${WEEK}&today=2026-10-14`, { token });
    expect(res.status).toBe(200);
    expect(res.json).toMatchObject({
      weekStart: WEEK,
      weekEnd: "2026-10-18",
      totalSeconds: 0,
      sessionCount: 0,
      bySubject: [],
      assignmentsCompleted: 0,
      streak: { current: 0, longest: 0 },
      dailyGoalMinutes: 60,
    });
    expect(res.json.days).toEqual(
      Array.from({ length: 7 }, (_, i) => ({ date: addDays(WEEK, i), seconds: 0 })),
    );
  });

  test("aggregates the week with inclusive Monday–Sunday boundaries", async () => {
    const { token } = await signUp();
    const maths = await createSubject(token, { name: "Maths", color: "sky" });
    const art = await createSubject(token, { name: "Art", color: "peach" });
    const sessions = [
      { day: "2026-10-11", durationSeconds: 3600, subjectId: maths.id }, // Sunday before: excluded
      { day: "2026-10-12", durationSeconds: 1500, subjectId: maths.id }, // Monday: included
      { day: "2026-10-12", durationSeconds: 600, subjectId: art.id },
      { day: "2026-10-15", durationSeconds: 2700, subjectId: maths.id },
      { day: "2026-10-18", durationSeconds: 900, subjectId: null }, // Sunday: included
      { day: "2026-10-19", durationSeconds: 3600, subjectId: art.id }, // next Monday: excluded
    ];
    for (const s of sessions) {
      await call("POST", "/api/focus-sessions", {
        token,
        body: sessionBody({ ...s, startedAt: `${s.day}T08:00:00Z` }),
      });
    }
    await call("POST", "/api/assignments", { token, body: assignmentBody(maths.id, { completedOn: "2026-10-12" }) });
    await call("POST", "/api/assignments", { token, body: assignmentBody(maths.id, { completedOn: "2026-10-18" }) });
    await call("POST", "/api/assignments", { token, body: assignmentBody(maths.id, { completedOn: "2026-10-19" }) });
    await call("POST", "/api/assignments", { token, body: assignmentBody(art.id) });

    const res = await call("GET", `/api/insights?week=${WEEK}&today=2026-10-18`, { token });
    expect(res.json.totalSeconds).toBe(1500 + 600 + 2700 + 900);
    expect(res.json.sessionCount).toBe(4);
    expect(res.json.days.map((d: { seconds: number }) => d.seconds)).toEqual([2100, 0, 0, 2700, 0, 0, 900]);
    expect(res.json.bySubject).toEqual([
      { subject: { id: maths.id, name: "Maths", color: "sky", icon: "book" }, seconds: 4200 },
      { subject: null, seconds: 900 },
      { subject: { id: art.id, name: "Art", color: "peach", icon: "book" }, seconds: 600 },
    ]);
    expect(res.json.assignmentsCompleted).toBe(2);
    expect(typeof res.json.totalSeconds).toBe("number");
  });

  test("computes the current and longest streak", async () => {
    const { token } = await signUp();
    const days = ["2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-10", "2026-10-12", "2026-10-13"];
    for (const day of days) {
      await call("POST", "/api/focus-sessions", { token, body: sessionBody({ day, startedAt: `${day}T08:00:00Z` }) });
    }
    // Today has no session yet, so the streak ending yesterday is still alive.
    const alive = await call("GET", `/api/insights?week=${WEEK}&today=2026-10-14`, { token });
    expect(alive.json.streak).toEqual({ current: 2, longest: 4 });
    // Sessions after "today" are ignored, and a gap of a full day breaks the streak.
    const broken = await call("GET", `/api/insights?week=${WEEK}&today=2026-10-11`, { token });
    expect(broken.json.streak).toEqual({ current: 1, longest: 4 });
    const lapsed = await call("GET", `/api/insights?week=${WEEK}&today=2026-10-16`, { token });
    expect(lapsed.json.streak.current).toBe(0);
  });

  test("large totals are returned as numbers", async () => {
    const { token } = await signUp();
    for (let i = 0; i < 6; i++) {
      await call("POST", "/api/focus-sessions", { token, body: sessionBody({ durationSeconds: 4 * 3600 }) });
    }
    const res = await call("GET", `/api/insights?week=${WEEK}`, { token });
    expect(res.json.totalSeconds).toBe(6 * 4 * 3600);
    expect(res.json.days[2].seconds).toBe(6 * 4 * 3600);
  });

  test("reflects the user's daily goal", async () => {
    const { token } = await signUp();
    await call("PATCH", "/api/me", { token, body: { dailyFocusGoalMinutes: 150 } });
    const res = await call("GET", `/api/insights?week=${WEEK}`, { token });
    expect(res.json.dailyGoalMinutes).toBe(150);
  });
});

describe("streak helper", () => {
  test("handles empty input, duplicates and month boundaries", () => {
    expect(streaks([], "2026-10-14")).toEqual({ current: 0, longest: 0 });
    expect(streaks(["2026-10-14", "2026-10-14"], "2026-10-14")).toEqual({ current: 1, longest: 1 });
    expect(streaks(["2026-09-30", "2026-10-01", "2026-10-02"], "2026-10-02")).toEqual({ current: 3, longest: 3 });
    expect(streaks(["2024-02-28", "2024-02-29", "2024-03-01"], "2024-03-01").current).toBe(3);
    expect(streaks(["2026-12-31", "2027-01-01"], "2027-01-02")).toEqual({ current: 2, longest: 2 });
  });
});

describe("profile", () => {
  test("get and update the profile", async () => {
    const { token, credentials } = await signUp();
    const me = await call("GET", "/api/me", { token });
    expect(me.json.profile).toMatchObject({ name: "Ayush", email: credentials.email, dailyFocusGoalMinutes: 60 });

    const updated = await call("PATCH", "/api/me", {
      token,
      body: { name: "  Ayush S  ", dailyFocusGoalMinutes: 120 },
    });
    expect(updated.json.profile).toMatchObject({ name: "Ayush S", dailyFocusGoalMinutes: 120 });

    const goalOnly = await call("PATCH", "/api/me", { token, body: { dailyFocusGoalMinutes: 45 } });
    expect(goalOnly.json.profile).toMatchObject({ name: "Ayush S", dailyFocusGoalMinutes: 45 });
    expect((await call("GET", "/api/me", { token })).json.profile.dailyFocusGoalMinutes).toBe(45);
  });

  test.each([
    ["goal too small", { dailyFocusGoalMinutes: 4 }],
    ["goal too large", { dailyFocusGoalMinutes: 721 }],
    ["fractional goal", { dailyFocusGoalMinutes: 30.5 }],
    ["empty name", { name: "   " }],
    ["long name", { name: "x".repeat(61) }],
    ["email change", { email: "new@example.com" }],
    ["empty body", {}],
  ])("rejects %s", async (_, body) => {
    const { token } = await signUp();
    const res = await call("PATCH", "/api/me", { token, body });
    expect(res.status).toBe(400);
  });

  test("deleting a user cascades to all their data", async () => {
    const { token, user } = await signUp();
    const subject = await createSubject(token);
    await call("POST", "/api/assignments", { token, body: assignmentBody(subject.id) });
    await call("POST", "/api/focus-sessions", { token, body: sessionBody({ subjectId: subject.id }) });
    const { sql } = await import("../src/db/client");
    await sql`DELETE FROM "user" WHERE id = ${user.id}`;
    const [row] = await sql`
      SELECT (SELECT count(*) FROM subject WHERE user_id = ${user.id})::int
           + (SELECT count(*) FROM assignment WHERE user_id = ${user.id})::int
           + (SELECT count(*) FROM focus_session WHERE user_id = ${user.id})::int AS count`;
    expect(row?.count).toBe(0);
  });
});
