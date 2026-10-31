import { and, between, count, desc, eq, isNotNull, lte, sql } from "drizzle-orm";
import { Hono } from "hono";
import { db } from "../db/client";
import { assignment, focusSession, subject, type FocusSession, type Subject } from "../db/schema";
import { addDays, streaks, todayUtc } from "../dates";
import { apiError, isForeignKeyViolation, type AppEnv } from "../http";
import { validate } from "../middleware";
import { focusInput, insightsQuery, weekQuery } from "../validation";
import { toSubjectSummary } from "./subjects";

/** Sessions can't claim to start more than this far in the future (allows for clock skew). */
const FUTURE_SKEW_MS = 5 * 60 * 1000;

function toDto(row: FocusSession, owner: Subject | null) {
  return {
    id: row.id,
    subject: owner ? toSubjectSummary(owner) : null,
    startedAt: row.startedAt.toISOString(),
    durationSeconds: row.durationSeconds,
    day: row.day,
  };
}

const UNKNOWN_SUBJECT = () => apiError(400, "bad_request", "subjectId: Subject not found");

/** Sums are bigint in Postgres; postgres-js returns them as strings, so map back to numbers. */
const totalSeconds = sql<number>`coalesce(sum(${focusSession.durationSeconds}), 0)::bigint`.mapWith(Number);

export const focusRoutes = new Hono<AppEnv>()
  .get("/focus-sessions", validate("query", weekQuery), async (c) => {
    const { week } = c.req.valid("query");
    const rows = await db
      .select({ session: focusSession, subject })
      .from(focusSession)
      .leftJoin(subject, eq(subject.id, focusSession.subjectId))
      .where(and(eq(focusSession.userId, c.get("user").id), between(focusSession.day, week, addDays(week, 6))))
      .orderBy(desc(focusSession.startedAt));
    return c.json({ sessions: rows.map((r) => toDto(r.session, r.subject)) });
  })

  .post("/focus-sessions", validate("json", focusInput), async (c) => {
    const input = c.req.valid("json");
    const userId = c.get("user").id;
    const startedAt = new Date(input.startedAt);
    if (startedAt.getTime() > Date.now() + FUTURE_SKEW_MS) {
      throw apiError(400, "bad_request", "startedAt: Session can't start in the future");
    }

    let owner: Subject | null = null;
    if (input.subjectId) {
      owner = (await db.query.subject.findFirst({
        where: and(eq(subject.id, input.subjectId), eq(subject.userId, userId)),
      })) ?? null;
      if (!owner) throw UNKNOWN_SUBJECT();
    }

    try {
      const [row] = await db
        .insert(focusSession)
        .values({
          ...(input.id ? { id: input.id } : {}),
          userId,
          subjectId: owner?.id ?? null,
          startedAt,
          durationSeconds: input.durationSeconds,
          day: input.day,
        })
        .onConflictDoNothing({ target: focusSession.id })
        .returning();
      if (row) return c.json({ session: toDto(row, owner) }, 201);

      // Same id again: a retry of an upload that already succeeded. Only the owner gets it back.
      const [existing] = await db
        .select({ session: focusSession, subject })
        .from(focusSession)
        .leftJoin(subject, eq(subject.id, focusSession.subjectId))
        .where(and(eq(focusSession.id, input.id!), eq(focusSession.userId, userId)));
      if (!existing) throw apiError(409, "conflict", "That session id is already taken");
      return c.json({ session: toDto(existing.session, existing.subject) }, 200);
    } catch (error) {
      if (isForeignKeyViolation(error)) throw UNKNOWN_SUBJECT();
      throw error;
    }
  })

  .get("/insights", validate("query", insightsQuery), async (c) => {
    const { week, today = todayUtc() } = c.req.valid("query");
    const user = c.get("user");
    const weekEnd = addDays(week, 6);
    const inWeek = and(eq(focusSession.userId, user.id), between(focusSession.day, week, weekEnd));

    const [totals, byDay, bySubject, completed, studyDays] = await Promise.all([
      db.select({ totalSeconds, sessions: count() }).from(focusSession).where(inWeek),
      db
        .select({ day: focusSession.day, seconds: totalSeconds })
        .from(focusSession)
        .where(inWeek)
        .groupBy(focusSession.day),
      db
        .select({ subject, seconds: totalSeconds })
        .from(focusSession)
        .leftJoin(subject, eq(subject.id, focusSession.subjectId))
        .where(inWeek)
        .groupBy(subject.id)
        .orderBy(desc(totalSeconds)),
      db
        .select({ value: count() })
        .from(assignment)
        .where(
          and(
            eq(assignment.userId, user.id),
            isNotNull(assignment.completedOn),
            between(assignment.completedOn, week, weekEnd),
          ),
        ),
      db
        .selectDistinct({ day: focusSession.day })
        .from(focusSession)
        .where(and(eq(focusSession.userId, user.id), lte(focusSession.day, today))),
    ]);

    const secondsByDay = new Map(byDay.map((row) => [row.day, row.seconds]));
    const days = Array.from({ length: 7 }, (_, i) => {
      const date = addDays(week, i);
      return { date, seconds: secondsByDay.get(date) ?? 0 };
    });

    return c.json({
      weekStart: week,
      weekEnd,
      totalSeconds: totals[0]?.totalSeconds ?? 0,
      sessionCount: totals[0]?.sessions ?? 0,
      days,
      bySubject: bySubject.map((row) => ({
        subject: row.subject ? toSubjectSummary(row.subject) : null,
        seconds: row.seconds,
      })),
      assignmentsCompleted: completed[0]?.value ?? 0,
      streak: streaks(
        studyDays.map((row) => row.day),
        today,
      ),
      dailyGoalMinutes: user.dailyFocusGoalMinutes ?? 0,
    });
  });
