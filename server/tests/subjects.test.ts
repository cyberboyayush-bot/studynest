import { describe, expect, test } from "bun:test";
import { assignmentBody, call, createSubject, signUp } from "./helpers";

describe("subjects CRUD", () => {
  test("create, list, update and delete a subject", async () => {
    const { token } = await signUp();

    const created = await call("POST", "/api/subjects", {
      token,
      body: { name: "  Physics  ", color: "mint", icon: "flask" },
    });
    expect(created.status).toBe(201);
    expect(created.json.subject).toMatchObject({
      name: "Physics",
      color: "mint",
      icon: "flask",
      openCount: 0,
      totalCount: 0,
    });
    const id = created.json.subject.id;

    const list = await call("GET", "/api/subjects", { token });
    expect(list.json.subjects).toHaveLength(1);

    const updated = await call("PATCH", `/api/subjects/${id}`, { token, body: { color: "peach" } });
    expect(updated.status).toBe(200);
    expect(updated.json.subject).toMatchObject({ name: "Physics", color: "peach", icon: "flask" });

    expect((await call("DELETE", `/api/subjects/${id}`, { token })).status).toBe(204);
    expect((await call("GET", `/api/subjects/${id}`, { token })).status).toBe(404);
    expect((await call("DELETE", `/api/subjects/${id}`, { token })).status).toBe(404);
  });

  test("lists subjects alphabetically with open and total assignment counts", async () => {
    const { token } = await signUp();
    const zoology = await createSubject(token, { name: "zoology" });
    const art = await createSubject(token, { name: "Art" });
    await call("POST", "/api/assignments", { token, body: assignmentBody(zoology.id) });
    await call("POST", "/api/assignments", {
      token,
      body: assignmentBody(zoology.id, { completedOn: "2026-10-18" }),
    });

    const res = await call("GET", "/api/subjects", { token });
    expect(res.json.subjects.map((s: { name: string }) => s.name)).toEqual(["Art", "zoology"]);
    expect(res.json.subjects[1]).toMatchObject({ id: zoology.id, openCount: 1, totalCount: 2 });
    expect(res.json.subjects[0]).toMatchObject({ id: art.id, openCount: 0, totalCount: 0 });
  });

  test("rejects duplicate names case-insensitively with 409", async () => {
    const { token } = await signUp();
    await createSubject(token, { name: "Chemistry" });
    const res = await call("POST", "/api/subjects", {
      token,
      body: { name: "CHEMISTRY", color: "sky", icon: "flask" },
    });
    expect(res.status).toBe(409);
    expect(res.json.error.code).toBe("conflict");
  });

  test("renaming onto an existing name is a conflict", async () => {
    const { token } = await signUp();
    await createSubject(token, { name: "Maths" });
    const other = await createSubject(token, { name: "Music" });
    const res = await call("PATCH", `/api/subjects/${other.id}`, { token, body: { name: "maths" } });
    expect(res.status).toBe(409);
  });

  test("different users may reuse the same subject name", async () => {
    const alice = await signUp();
    const bob = await signUp();
    await createSubject(alice.token, { name: "Biology" });
    await createSubject(bob.token, { name: "Biology" });
  });

  test("deleting a subject deletes its assignments", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token);
    const created = await call("POST", "/api/assignments", { token, body: assignmentBody(subject.id) });
    await call("DELETE", `/api/subjects/${subject.id}`, { token });
    expect((await call("GET", `/api/assignments/${created.json.assignment.id}`, { token })).status).toBe(404);
  });

  test.each([
    ["blank name", { name: "   " }],
    ["long name", { name: "x".repeat(41) }],
    ["unknown colour", { color: "neon" }],
    ["unknown icon", { icon: "rocket" }],
    ["unknown field", { userId: "someone-else" }],
  ])("rejects %s", async (_, override) => {
    const { token } = await signUp();
    const res = await call("POST", "/api/subjects", {
      token,
      body: { name: "Maths", color: "lilac", icon: "book", ...override },
    });
    expect(res.status).toBe(400);
    expect(res.json.error.code).toBe("bad_request");
  });

  test("rejects an empty update and malformed ids", async () => {
    const { token } = await signUp();
    const subject = await createSubject(token);
    expect((await call("PATCH", `/api/subjects/${subject.id}`, { token, body: {} })).status).toBe(400);
    expect((await call("GET", "/api/subjects/not-a-uuid", { token })).status).toBe(400);
  });
});
