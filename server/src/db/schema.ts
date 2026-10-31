import { sql } from "drizzle-orm";
import {
  boolean,
  check,
  date,
  index,
  integer,
  pgTable,
  text,
  timestamp,
  uniqueIndex,
  uuid,
  varchar,
} from "drizzle-orm/pg-core";
import {
  DEFAULT_DAILY_GOAL_MINUTES,
  MAX_SESSION_SECONDS,
  MIN_SESSION_SECONDS,
  PRIORITIES,
  SUBJECT_COLORS,
  SUBJECT_ICONS,
} from "../constants";

const timestamps = {
  createdAt: timestamp({ withTimezone: true }).defaultNow().notNull(),
  updatedAt: timestamp({ withTimezone: true })
    .defaultNow()
    .$onUpdate(() => new Date())
    .notNull(),
};

const inList = (column: string, values: readonly string[]) =>
  sql.raw(`${column} IN (${values.map((v) => `'${v}'`).join(", ")})`);

// Better Auth core tables (user, session, account, verification).

export const user = pgTable("user", {
  id: text().primaryKey(),
  name: text().notNull(),
  email: text().notNull().unique(),
  emailVerified: boolean().default(false).notNull(),
  image: text(),
  /** Target minutes of focused study per day. */
  dailyFocusGoalMinutes: integer().default(DEFAULT_DAILY_GOAL_MINUTES).notNull(),
  ...timestamps,
});

export const session = pgTable(
  "session",
  {
    id: text().primaryKey(),
    expiresAt: timestamp({ withTimezone: true }).notNull(),
    token: text().notNull().unique(),
    ipAddress: text(),
    userAgent: text(),
    userId: text()
      .notNull()
      .references(() => user.id, { onDelete: "cascade" }),
    ...timestamps,
  },
  (table) => [index("session_user_id_idx").on(table.userId)],
);

export const account = pgTable(
  "account",
  {
    id: text().primaryKey(),
    accountId: text().notNull(),
    providerId: text().notNull(),
    userId: text()
      .notNull()
      .references(() => user.id, { onDelete: "cascade" }),
    accessToken: text(),
    refreshToken: text(),
    idToken: text(),
    accessTokenExpiresAt: timestamp({ withTimezone: true }),
    refreshTokenExpiresAt: timestamp({ withTimezone: true }),
    scope: text(),
    password: text(),
    ...timestamps,
  },
  (table) => [index("account_user_id_idx").on(table.userId)],
);

export const verification = pgTable(
  "verification",
  {
    id: text().primaryKey(),
    identifier: text().notNull(),
    value: text().notNull(),
    expiresAt: timestamp({ withTimezone: true }).notNull(),
    ...timestamps,
  },
  (table) => [index("verification_identifier_idx").on(table.identifier)],
);

// App tables.

export const subject = pgTable(
  "subject",
  {
    id: uuid().primaryKey().defaultRandom(),
    userId: text()
      .notNull()
      .references(() => user.id, { onDelete: "cascade" }),
    name: varchar({ length: 40 }).notNull(),
    color: text({ enum: SUBJECT_COLORS }).notNull(),
    icon: text({ enum: SUBJECT_ICONS }).notNull(),
    ...timestamps,
  },
  (table) => [
    uniqueIndex("subject_user_name_idx").on(table.userId, sql`lower(${table.name})`),
    check("subject_color_valid", inList("color", SUBJECT_COLORS)),
    check("subject_icon_valid", inList("icon", SUBJECT_ICONS)),
  ],
);

export const assignment = pgTable(
  "assignment",
  {
    id: uuid().primaryKey().defaultRandom(),
    userId: text()
      .notNull()
      .references(() => user.id, { onDelete: "cascade" }),
    subjectId: uuid()
      .notNull()
      .references(() => subject.id, { onDelete: "cascade" }),
    title: varchar({ length: 100 }).notNull(),
    notes: varchar({ length: 500 }).default("").notNull(),
    dueOn: date({ mode: "string" }).notNull(),
    priority: text({ enum: PRIORITIES }).default("medium").notNull(),
    /** The student's local date when it was marked done; null while open. */
    completedOn: date({ mode: "string" }),
    ...timestamps,
  },
  (table) => [
    index("assignment_user_due_idx").on(table.userId, table.dueOn),
    index("assignment_subject_idx").on(table.subjectId),
    check("assignment_priority_valid", inList("priority", PRIORITIES)),
  ],
);

export const focusSession = pgTable(
  "focus_session",
  {
    id: uuid().primaryKey().defaultRandom(),
    userId: text()
      .notNull()
      .references(() => user.id, { onDelete: "cascade" }),
    subjectId: uuid().references(() => subject.id, { onDelete: "set null" }),
    startedAt: timestamp({ withTimezone: true }).notNull(),
    durationSeconds: integer().notNull(),
    /** The student's local calendar date the session started on, used for daily totals. */
    day: date({ mode: "string" }).notNull(),
    ...timestamps,
  },
  (table) => [
    index("focus_session_user_day_idx").on(table.userId, table.day),
    index("focus_session_subject_idx").on(table.subjectId),
    check(
      "focus_session_duration_range",
      sql`${table.durationSeconds} >= ${sql.raw(String(MIN_SESSION_SECONDS))} AND ${table.durationSeconds} <= ${sql.raw(String(MAX_SESSION_SECONDS))}`,
    ),
  ],
);

export type Subject = typeof subject.$inferSelect;
export type Assignment = typeof assignment.$inferSelect;
export type FocusSession = typeof focusSession.$inferSelect;
