import { and, asc, count, eq, sql } from "drizzle-orm";
import { Hono } from "hono";
import { db } from "../db/client";
import { assignment, subject, type Subject } from "../db/schema";
import { apiError, isUniqueViolation, type AppEnv } from "../http";
import { validate } from "../middleware";
import { idParam, subjectInput, subjectUpdate } from "../validation";

export function toSubjectSummary(row: Pick<Subject, "id" | "name" | "color" | "icon">) {
  return { id: row.id, name: row.name, color: row.color, icon: row.icon };
}

function toDto(row: Subject, openCount: number, totalCount: number) {
  return {
    ...toSubjectSummary(row),
    openCount,
    totalCount,
    createdAt: row.createdAt.toISOString(),
  };
}

const DUPLICATE = () => apiError(409, "conflict", "You already have a subject with this name");

async function withCounts(userId: string, subjectId?: string) {
  const openCount = sql<number>`count(${assignment.id}) filter (where ${assignment.completedOn} is null)`.mapWith(Number);
  const totalCount = count(assignment.id);
  const filters = [eq(subject.userId, userId)];
  if (subjectId) filters.push(eq(subject.id, subjectId));
  return db
    .select({ subject, openCount, totalCount })
    .from(subject)
    .leftJoin(assignment, eq(assignment.subjectId, subject.id))
    .where(and(...filters))
    .groupBy(subject.id)
    .orderBy(asc(sql`lower(${subject.name})`));
}

export const subjectRoutes = new Hono<AppEnv>()
  .get("/subjects", async (c) => {
    const rows = await withCounts(c.get("user").id);
    return c.json({ subjects: rows.map((r) => toDto(r.subject, r.openCount, r.totalCount)) });
  })

  .post("/subjects", validate("json", subjectInput), async (c) => {
    try {
      const [row] = await db
        .insert(subject)
        .values({ ...c.req.valid("json"), userId: c.get("user").id })
        .returning();
      return c.json({ subject: toDto(row!, 0, 0) }, 201);
    } catch (error) {
      if (isUniqueViolation(error)) throw DUPLICATE();
      throw error;
    }
  })

  .get("/subjects/:id", validate("param", idParam), async (c) => {
    const [row] = await withCounts(c.get("user").id, c.req.valid("param").id);
    if (!row) throw apiError(404, "not_found", "Subject not found");
    return c.json({ subject: toDto(row.subject, row.openCount, row.totalCount) });
  })

  .patch("/subjects/:id", validate("param", idParam), validate("json", subjectUpdate), async (c) => {
    const { id } = c.req.valid("param");
    const userId = c.get("user").id;
    try {
      const [updated] = await db
        .update(subject)
        .set(c.req.valid("json"))
        .where(and(eq(subject.id, id), eq(subject.userId, userId)))
        .returning({ id: subject.id });
      if (!updated) throw apiError(404, "not_found", "Subject not found");
    } catch (error) {
      if (isUniqueViolation(error)) throw DUPLICATE();
      throw error;
    }
    const [row] = await withCounts(userId, id);
    return c.json({ subject: toDto(row!.subject, row!.openCount, row!.totalCount) });
  })

  .delete("/subjects/:id", validate("param", idParam), async (c) => {
    const { id } = c.req.valid("param");
    const [row] = await db
      .delete(subject)
      .where(and(eq(subject.id, id), eq(subject.userId, c.get("user").id)))
      .returning({ id: subject.id });
    if (!row) throw apiError(404, "not_found", "Subject not found");
    return c.body(null, 204);
  });
