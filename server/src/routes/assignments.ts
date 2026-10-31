import { and, asc, eq, ilike, isNotNull, isNull, or, sql, type SQL } from "drizzle-orm";
import { Hono } from "hono";
import { db } from "../db/client";
import { assignment, subject, type Assignment, type Subject } from "../db/schema";
import { apiError, escapeLike, isForeignKeyViolation, type AppEnv } from "../http";
import { validate } from "../middleware";
import { assignmentInput, assignmentQuery, assignmentUpdate, idParam } from "../validation";
import { toSubjectSummary } from "./subjects";

const LIST_LIMIT = 500;

function toDto(row: Assignment, owner: Subject) {
  return {
    id: row.id,
    title: row.title,
    notes: row.notes,
    dueOn: row.dueOn,
    priority: row.priority,
    completedOn: row.completedOn,
    subject: toSubjectSummary(owner),
    createdAt: row.createdAt.toISOString(),
    updatedAt: row.updatedAt.toISOString(),
  };
}

const UNKNOWN_SUBJECT = () => apiError(400, "bad_request", "subjectId: Subject not found");
const NOT_FOUND = () => apiError(404, "not_found", "Assignment not found");

/** Assignments may only point at subjects owned by the same user. */
async function assertOwnSubject(userId: string, subjectId: string) {
  const row = await db.query.subject.findFirst({
    columns: { id: true },
    where: and(eq(subject.id, subjectId), eq(subject.userId, userId)),
  });
  if (!row) throw UNKNOWN_SUBJECT();
}

async function findOne(userId: string, id: string) {
  const [row] = await db
    .select({ assignment, subject })
    .from(assignment)
    .innerJoin(subject, eq(subject.id, assignment.subjectId))
    .where(and(eq(assignment.id, id), eq(assignment.userId, userId)));
  return row;
}

const priorityRank = sql`case ${assignment.priority} when 'high' then 0 when 'medium' then 1 else 2 end`;

export const assignmentRoutes = new Hono<AppEnv>()
  .get("/assignments", validate("query", assignmentQuery), async (c) => {
    const { subjectId, status = "all", q } = c.req.valid("query");
    const filters: SQL[] = [eq(assignment.userId, c.get("user").id)];
    if (subjectId) filters.push(eq(assignment.subjectId, subjectId));
    if (status === "open") filters.push(isNull(assignment.completedOn));
    if (status === "done") filters.push(isNotNull(assignment.completedOn));
    if (q) {
      const pattern = `%${escapeLike(q)}%`;
      filters.push(or(ilike(assignment.title, pattern), ilike(assignment.notes, pattern))!);
    }

    const rows = await db
      .select({ assignment, subject })
      .from(assignment)
      .innerJoin(subject, eq(subject.id, assignment.subjectId))
      .where(and(...filters))
      .orderBy(asc(assignment.dueOn), priorityRank, asc(assignment.createdAt))
      .limit(LIST_LIMIT);

    return c.json({ assignments: rows.map((r) => toDto(r.assignment, r.subject)) });
  })

  .post("/assignments", validate("json", assignmentInput), async (c) => {
    const input = c.req.valid("json");
    const userId = c.get("user").id;
    await assertOwnSubject(userId, input.subjectId);
    try {
      const [created] = await db
        .insert(assignment)
        .values({ ...input, userId })
        .returning({ id: assignment.id });
      const row = await findOne(userId, created!.id);
      return c.json({ assignment: toDto(row!.assignment, row!.subject) }, 201);
    } catch (error) {
      if (isForeignKeyViolation(error)) throw UNKNOWN_SUBJECT();
      throw error;
    }
  })

  .get("/assignments/:id", validate("param", idParam), async (c) => {
    const row = await findOne(c.get("user").id, c.req.valid("param").id);
    if (!row) throw NOT_FOUND();
    return c.json({ assignment: toDto(row.assignment, row.subject) });
  })

  .patch("/assignments/:id", validate("param", idParam), validate("json", assignmentUpdate), async (c) => {
    const { id } = c.req.valid("param");
    const input = c.req.valid("json");
    const userId = c.get("user").id;
    if (input.subjectId) await assertOwnSubject(userId, input.subjectId);
    try {
      const [updated] = await db
        .update(assignment)
        .set(input)
        .where(and(eq(assignment.id, id), eq(assignment.userId, userId)))
        .returning({ id: assignment.id });
      if (!updated) throw NOT_FOUND();
    } catch (error) {
      if (isForeignKeyViolation(error)) throw UNKNOWN_SUBJECT();
      throw error;
    }
    const row = await findOne(userId, id);
    return c.json({ assignment: toDto(row!.assignment, row!.subject) });
  })

  .delete("/assignments/:id", validate("param", idParam), async (c) => {
    const [row] = await db
      .delete(assignment)
      .where(and(eq(assignment.id, c.req.valid("param").id), eq(assignment.userId, c.get("user").id)))
      .returning({ id: assignment.id });
    if (!row) throw NOT_FOUND();
    return c.body(null, 204);
  });
