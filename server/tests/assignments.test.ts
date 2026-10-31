import { describe, expect, test } from "bun:test";
import { assignmentBody, call, createSubject, signUp } from "./helpers";

describe("assignments CRUD", () => {
  test("create, read, update and delete an assignment", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token, { name: "Algorithms", color: "sky", icon: "code" });

    const created = await call("POST", "/api/assignments", { token, body: assignmentBody(subject.id) });
    expect(created.status).toBe(201);
    const id = created.json.assignment.id;
    expect(created.json.assignment).toMatchObject({
      title: "Problem set 3",
      dueOn: "2026-10-20",
      priority: "high",
      notes: "Odd questions",
      completedOn: null,
      subject: { id: subject.id, name: "Algorithms", color: "sky", icon: "code" },
    });

    expect((await call("GET", `/api/assignments/${id}`, { token })).json.assignment.id).toBe(id);

    const done = await call("PATCH", `/api/assignments/${id}`, { token, body: { completedOn: "2026-10-19" } });
    expect(done.status).toBe(200);
    expect(done.json.assignment).toMatchObject({ completedOn: "2026-10-19", priority: "high", notes: "Odd questions" });

    const reopened = await call("PATCH", `/api/assignments/${id}`, { token, body: { completedOn: null } });
    expect(reopened.json.assignment.completedOn).toBeNull();

    const renamed = await call("PATCH", `/api/assignments/${id}`, { token, body: { title: "  Set 4  " } });
    expect(renamed.json.assignment).toMatchObject({ title: "Set 4", priority: "high", dueOn: "2026-10-20" });

    expect((await call("DELETE", `/api/assignments/${id}`, { token })).status).toBe(204);
    expect((await call("GET", `/api/assignments/${id}`, { token })).status).toBe(404);
    expect((await call("DELETE", `/api/assignments/${id}`, { token })).status).toBe(404);
  });

  test("create applies defaults for optional fields", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token);
    const res = await call("POST", "/api/assignments", {
      token,
      body: { title: "Read chapter 2", subjectId: subject.id, dueOn: "2026-11-01" },
    });
    expect(res.status).toBe(201);
    expect(res.json.assignment).toMatchObject({ priority: "medium", notes: "", completedOn: null });
  });

  test("a partial update never resets omitted fields to defaults", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token);
    const created = await call("POST", "/api/assignments", {
      token,
      body: assignmentBody(subject.id, { priority: "low", notes: "Keep me" }),
    });
    const res = await call("PATCH", `/api/assignments/${created.json.assignment.id}`, {
      token,
      body: { dueOn: "2026-12-01" },
    });
    expect(res.json.assignment).toMatchObject({ priority: "low", notes: "Keep me", dueOn: "2026-12-01" });
  });

  test("can move an assignment to another subject", async () => {
    const { token } = await signUp();
    const a = await createSubject(token);
    const b = await createSubject(token);
    const created = await call("POST", "/api/assignments", { token, body: assignmentBody(a.id) });
    const moved = await call("PATCH", `/api/assignments/${created.json.assignment.id}`, {
      token,
      body: { subjectId: b.id },
    });
    expect(moved.json.assignment.subject.id).toBe(b.id);
  });

  test("rejects an unknown subject", async () => {
    const { token } = await signUp();
    const res = await call("POST", "/api/assignments", {
      token,
      body: assignmentBody("00000000-0000-4000-8000-000000000000"),
    });
    expect(res.status).toBe(400);
    expect(res.json.error.message).toContain("Subject not found");
  });

  test.each([
    ["blank title", { title: "  " }],
    ["long title", { title: "x".repeat(101) }],
    ["long notes", { notes: "x".repeat(501) }],
    ["bad priority", { priority: "urgent" }],
    ["bad date", { dueOn: "20-10-2026" }],
    ["impossible date", { dueOn: "2026-02-30" }],
    ["date out of range", { dueOn: "1999-12-31" }],
    ["bad completedOn", { completedOn: "yesterday" }],
    ["malformed subjectId", { subjectId: "abc" }],
    ["unknown field", { userId: "someone-else" }],
  ])("rejects %s", async (_, override) => {
    const { token } = await signUp();
    const subject = await createSubject(token);
    const res = await call("POST", "/api/assignments", { token, body: assignmentBody(subject.id, override) });
    expect(res.status).toBe(400);
    expect(res.json.error.code).toBe("bad_request");
  });

  test("rejects an empty update and invalid JSON", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token);
    const created = await call("POST", "/api/assignments", { token, body: assignmentBody(subject.id) });
    const id = created.json.assignment.id;
    expect((await call("PATCH", `/api/assignments/${id}`, { token, body: {} })).status).toBe(400);
    const invalid = await call("POST", "/api/assignments", {
      token,
      headers: { "Content-Type": "application/json" },
    });
    expect(invalid.status).toBe(400);
  });
});

describe("listing and filtering assignments", () => {
  async function seed() {
    const { token } = await signUp();
    const maths = await createSubject(token, { name: "Maths" });
    const english = await createSubject(token, { name: "English" });
    const bodies = [
      assignmentBody(maths.id, { title: "Calculus worksheet", dueOn: "2026-10-22", priority: "low" }),
      assignmentBody(maths.id, { title: "Proofs 100% done", dueOn: "2026-10-22", priority: "high" }),
      assignmentBody(english.id, { title: "Essay draft", dueOn: "2026-10-18", notes: "About calculus anxiety" }),
      assignmentBody(english.id, { title: "Poem", dueOn: "2026-10-25", completedOn: "2026-10-17" }),
    ];
    for (const body of bodies) await call("POST", "/api/assignments", { token, body });
    return { token, maths, english };
  }

  test("orders by due date, then priority", async () => {
    const { token } = await seed();
    const res = await call("GET", "/api/assignments", { token });
    expect(res.json.assignments.map((a: { title: string }) => a.title)).toEqual([
      "Essay draft",
      "Proofs 100% done",
      "Calculus worksheet",
      "Poem",
    ]);
  });

  test("filters by subject", async () => {
    const { token, english } = await seed();
    const res = await call("GET", `/api/assignments?subjectId=${english.id}`, { token });
    expect(res.json.assignments).toHaveLength(2);
  });

  test("filters by status", async () => {
    const { token } = await seed();
    expect((await call("GET", "/api/assignments?status=open", { token })).json.assignments).toHaveLength(3);
    const done = await call("GET", "/api/assignments?status=done", { token });
    expect(done.json.assignments.map((a: { title: string }) => a.title)).toEqual(["Poem"]);
    expect((await call("GET", "/api/assignments?status=all", { token })).json.assignments).toHaveLength(4);
  });

  test("searches title and notes case-insensitively", async () => {
    const { token } = await seed();
    const res = await call("GET", "/api/assignments?q=CALCULUS", { token });
    expect(res.json.assignments.map((a: { title: string }) => a.title).sort()).toEqual([
      "Calculus worksheet",
      "Essay draft",
    ]);
  });

  test("combines filters", async () => {
    const { token, maths } = await seed();
    const res = await call("GET", `/api/assignments?subjectId=${maths.id}&status=open&q=proof`, { token });
    expect(res.json.assignments).toHaveLength(1);
  });

  test("treats LIKE wildcards literally", async () => {
    const { token } = await seed();
    expect((await call("GET", "/api/assignments?q=%25", { token })).json.assignments).toHaveLength(1);
    expect((await call("GET", "/api/assignments?q=_", { token })).json.assignments).toHaveLength(0);
  });

  test("rejects bad query params", async () => {
    const { token } = await signUp();
    expect((await call("GET", "/api/assignments?status=late", { token })).status).toBe(400);
    expect((await call("GET", "/api/assignments?subjectId=nope", { token })).status).toBe(400);
    expect((await call("GET", "/api/assignments?sort=title", { token })).status).toBe(400);
  });
});

describe("isolation between users", () => {
  test("users cannot see, edit or delete each other's data", async () => {
    const alice = await signUp({ name: "Alice" });
    const bob = await signUp({ name: "Bob" });
    const subject = await createSubject(alice.token, { name: "Secret" });
    const created = await call("POST", "/api/assignments", {
      token: alice.token,
      body: assignmentBody(subject.id),
    });
    const id = created.json.assignment.id;

    expect((await call("GET", `/api/assignments/${id}`, { token: bob.token })).status).toBe(404);
    expect(
      (await call("PATCH", `/api/assignments/${id}`, { token: bob.token, body: { title: "Mine" } })).status,
    ).toBe(404);
    expect((await call("DELETE", `/api/assignments/${id}`, { token: bob.token })).status).toBe(404);
    expect((await call("GET", "/api/assignments", { token: bob.token })).json.assignments).toEqual([]);
    expect(
      (await call("GET", `/api/assignments?subjectId=${subject.id}`, { token: bob.token })).json.assignments,
    ).toEqual([]);

    expect((await call("GET", `/api/subjects/${subject.id}`, { token: bob.token })).status).toBe(404);
    expect(
      (await call("PATCH", `/api/subjects/${subject.id}`, { token: bob.token, body: { name: "Mine" } })).status,
    ).toBe(404);
    expect((await call("DELETE", `/api/subjects/${subject.id}`, { token: bob.token })).status).toBe(404);
    expect((await call("GET", "/api/subjects", { token: bob.token })).json.subjects).toEqual([]);

    const still = await call("GET", `/api/assignments/${id}`, { token: alice.token });
    expect(still.json.assignment.title).toBe("Problem set 3");
  });

  test("cannot attach assignments or sessions to another user's subject", async () => {
    const alice = await signUp();
    const bob = await signUp();
    const aliceSubject = await createSubject(alice.token);
    const bobSubject = await createSubject(bob.token);

    const create = await call("POST", "/api/assignments", {
      token: bob.token,
      body: assignmentBody(aliceSubject.id),
    });
    expect(create.status).toBe(400);

    const own = await call("POST", "/api/assignments", { token: bob.token, body: assignmentBody(bobSubject.id) });
    const move = await call("PATCH", `/api/assignments/${own.json.assignment.id}`, {
      token: bob.token,
      body: { subjectId: aliceSubject.id },
    });
    expect(move.status).toBe(400);

    const session = await call("POST", "/api/focus-sessions", {
      token: bob.token,
      body: { subjectId: aliceSubject.id, startedAt: "2026-10-14T09:00:00Z", durationSeconds: 1500, day: "2026-10-14" },
    });
    expect(session.status).toBe(400);
  });
});
