import { eq } from "drizzle-orm";
import { auth } from "../auth";
import type { Priority, SubjectColor, SubjectIcon } from "../constants";
import { db, sql } from "./client";
import { assignment, focusSession, subject, user } from "./schema";

const DEMO = { name: "Ayush", email: "demo@studynest.app", password: "demo12345" };

const SUBJECTS: [name: string, color: SubjectColor, icon: SubjectIcon][] = [
  ["Data Structures", "lilac", "code"],
  ["Linear Algebra", "sky", "calculator"],
  ["Organic Chemistry", "mint", "flask"],
  ["English Literature", "peach", "book"],
  ["World History", "butter", "history"],
];

/** [subject index, title, due in N days (negative = past), priority, completed N days ago or null, notes] */
type AssignmentSeed = [number, string, number, Priority, number | null, string];

const ASSIGNMENTS: AssignmentSeed[] = [
  // Overdue and still open
  [1, "Eigenvalues problem set", -2, "high", null, "Questions 4–12 from chapter 6."],
  [3, "Reading response: Middlemarch", -1, "medium", null, "300 words on Dorothea's choices."],
  // Due today
  [0, "Binary search tree lab", 0, "high", null, "Implement insert, delete and in-order traversal."],
  [2, "Pre-lab quiz: esterification", 0, "low", null, ""],
  // This week
  [4, "Essay outline: Treaty of Versailles", 2, "medium", null, "Three causes, one counter-argument."],
  [2, "Lab report: titration", 3, "high", null, "Include the error analysis table."],
  [0, "Graph traversal worksheet", 4, "medium", null, "BFS and DFS by hand on the sample graphs."],
  // Later
  [1, "Midterm revision sheet", 8, "high", null, "Determinants, rank, change of basis."],
  [3, "Poetry analysis: Keats odes", 10, "medium", null, ""],
  [4, "Source analysis: propaganda posters", 12, "low", null, "Pick two posters from the archive."],
  [0, "Hash map project", 15, "high", null, "Open addressing with linear probing."],
  [2, "Reaction mechanisms flashcards", 18, "low", null, ""],
  [1, "Group presentation slides", 21, "medium", null, "Applications of SVD."],
  // Completed
  [0, "Linked list quiz", -9, "medium", 10, ""],
  [1, "Matrix multiplication drills", -6, "low", 7, ""],
  [3, "Sonnet close reading", -5, "medium", 5, "Shakespeare, Sonnet 73."],
  [2, "Safety induction form", -4, "low", 4, ""],
  [4, "Timeline of the Cold War", -3, "medium", 2, ""],
  [0, "Stack and queue exercises", -2, "high", 1, ""],
  [2, "Nomenclature worksheet", 1, "medium", 0, "Finished early."],
];

/** Days ago that had study sessions: [days ago, subject index, minutes, start hour]. */
const SESSIONS: [number, number, number, number][] = [
  [0, 0, 25, 9],
  [0, 2, 20, 11],
  [1, 0, 45, 10], [1, 1, 30, 16], [1, 3, 25, 21],
  [2, 2, 50, 9], [2, 4, 25, 18],
  [3, 1, 60, 14], [3, 0, 25, 20],
  [4, 3, 45, 10], [4, 2, 15, 17],
  [5, 0, 30, 11],
  [6, 4, 40, 15], [6, 1, 25, 19],
  [8, 0, 50, 10], [8, 3, 25, 20],
  [9, 1, 45, 9],
  [10, 2, 30, 14], [10, 4, 25, 18],
  [11, 0, 60, 10],
  [13, 3, 35, 16], [13, 1, 25, 20],
  [14, 2, 45, 11],
  [15, 0, 25, 9], [15, 4, 30, 15],
  [16, 1, 50, 10],
  [18, 3, 25, 19],
  [19, 0, 45, 10], [19, 2, 20, 16],
  [20, 4, 30, 14],
];

function localDate(daysFromToday: number) {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  return date.toLocaleDateString("en-CA");
}

function localDateTime(daysAgo: number, hour: number) {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  date.setHours(hour, 0, 0, 0);
  // Never claim a session that hasn't happened yet.
  return date.getTime() > Date.now() ? new Date(Date.now() - 60 * 60 * 1000) : date;
}

const existing = await db.query.user.findFirst({ where: eq(user.email, DEMO.email) });
if (existing) await db.delete(user).where(eq(user.id, existing.id));

const { user: created } = await auth.api.signUpEmail({ body: DEMO });
await db.update(user).set({ dailyFocusGoalMinutes: 90 }).where(eq(user.id, created.id));

const subjects = await db
  .insert(subject)
  .values(SUBJECTS.map(([name, color, icon]) => ({ userId: created.id, name, color, icon })))
  .returning();
const subjectId = (index: number) => subjects[index]!.id;

await db.insert(assignment).values(
  ASSIGNMENTS.map(([s, title, dueIn, priority, completedAgo, notes]) => ({
    userId: created.id,
    subjectId: subjectId(s),
    title,
    dueOn: localDate(dueIn),
    priority,
    completedOn: completedAgo === null ? null : localDate(-completedAgo),
    notes,
  })),
);

await db.insert(focusSession).values(
  SESSIONS.map(([daysAgo, s, minutes, hour]) => {
    const startedAt = localDateTime(daysAgo, hour);
    return {
      userId: created.id,
      subjectId: subjectId(s),
      startedAt,
      durationSeconds: minutes * 60,
      day: startedAt.toLocaleDateString("en-CA"),
    };
  }),
);

await sql.end();
console.log(
  `Seeded ${subjects.length} subjects, ${ASSIGNMENTS.length} assignments and ${SESSIONS.length} focus sessions ` +
    `for ${DEMO.email} / ${DEMO.password}`,
);
