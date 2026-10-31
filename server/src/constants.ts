export const SUBJECT_COLORS = ["lilac", "peach", "mint", "butter", "sky", "rose"] as const;
export type SubjectColor = (typeof SUBJECT_COLORS)[number];

export const SUBJECT_ICONS = [
  "book",
  "calculator",
  "flask",
  "globe",
  "palette",
  "code",
  "music",
  "language",
  "history",
  "biology",
] as const;
export type SubjectIcon = (typeof SUBJECT_ICONS)[number];

export const PRIORITIES = ["low", "medium", "high"] as const;
export type Priority = (typeof PRIORITIES)[number];

export const DEFAULT_DAILY_GOAL_MINUTES = 60;
export const MIN_DAILY_GOAL_MINUTES = 5;
export const MAX_DAILY_GOAL_MINUTES = 720;

export const MIN_SESSION_SECONDS = 60;
export const MAX_SESSION_SECONDS = 4 * 60 * 60;
