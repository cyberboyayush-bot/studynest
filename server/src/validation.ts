import { z } from "zod";
import {
  MAX_DAILY_GOAL_MINUTES,
  MAX_SESSION_SECONDS,
  MIN_DAILY_GOAL_MINUTES,
  MIN_SESSION_SECONDS,
  PRIORITIES,
  SUBJECT_COLORS,
  SUBJECT_ICONS,
} from "./constants";
import { isMonday } from "./dates";

const nonEmpty = (value: object) => Object.keys(value).length > 0;
const NON_EMPTY_MESSAGE = "Provide at least one field to update";

export const isoDate = z.iso.date().refine((value) => {
  const year = Number(value.slice(0, 4));
  return year >= 2000 && year <= 2100;
}, "Date must be between the years 2000 and 2100");

export const idParam = z.object({ id: z.uuid() });

// Subjects

const subjectFields = z.strictObject({
  name: z.string().trim().min(1, "Name is required").max(40),
  color: z.enum(SUBJECT_COLORS),
  icon: z.enum(SUBJECT_ICONS),
});

export const subjectInput = subjectFields;
export const subjectUpdate = subjectFields.partial().refine(nonEmpty, NON_EMPTY_MESSAGE);

// Assignments

const assignmentFields = z.strictObject({
  title: z.string().trim().min(1, "Title is required").max(100),
  subjectId: z.uuid(),
  dueOn: isoDate,
  priority: z.enum(PRIORITIES),
  notes: z.string().trim().max(500),
  completedOn: isoDate.nullable(),
});

/** Defaults live only on the create schema so a PATCH never silently resets omitted fields. */
export const assignmentInput = assignmentFields.extend({
  priority: assignmentFields.shape.priority.default("medium"),
  notes: assignmentFields.shape.notes.default(""),
  completedOn: assignmentFields.shape.completedOn.default(null),
});

export const assignmentUpdate = assignmentFields.partial().refine(nonEmpty, NON_EMPTY_MESSAGE);

export const assignmentQuery = z.strictObject({
  subjectId: z.uuid().optional(),
  status: z.enum(["open", "done", "all"]).optional(),
  q: z.string().trim().max(60).optional(),
});

// Focus sessions and insights

const weekStart = isoDate.refine(isMonday, "Week must start on a Monday");

export const focusInput = z.strictObject({
  /** Optional client-generated id, so a retried upload of the same session is recorded only once. */
  id: z.uuid().optional(),
  subjectId: z.uuid().nullable().optional(),
  startedAt: z.iso.datetime({ offset: true }),
  durationSeconds: z.int().min(MIN_SESSION_SECONDS).max(MAX_SESSION_SECONDS),
  day: isoDate,
});

export const weekQuery = z.strictObject({ week: weekStart });

export const insightsQuery = z.strictObject({ week: weekStart, today: isoDate.optional() });

// Profile

export const profileUpdate = z
  .strictObject({
    name: z.string().trim().min(1).max(60),
    dailyFocusGoalMinutes: z.int().min(MIN_DAILY_GOAL_MINUTES).max(MAX_DAILY_GOAL_MINUTES),
  })
  .partial()
  .refine(nonEmpty, NON_EMPTY_MESSAGE);
