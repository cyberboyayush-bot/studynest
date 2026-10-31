/** Calendar helpers for plain `YYYY-MM-DD` dates. All math is done in UTC so DST never shifts a day. */

function toUtc(day: string): Date {
  const [y, m, d] = day.split("-").map(Number) as [number, number, number];
  return new Date(Date.UTC(y, m - 1, d));
}

function fromUtc(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function addDays(day: string, days: number): string {
  const date = toUtc(day);
  date.setUTCDate(date.getUTCDate() + days);
  return fromUtc(date);
}

/** 1 = Monday … 7 = Sunday (ISO-8601). */
export function isoWeekday(day: string): number {
  const weekday = toUtc(day).getUTCDay();
  return weekday === 0 ? 7 : weekday;
}

export function isMonday(day: string): boolean {
  return isoWeekday(day) === 1;
}

export function todayUtc(): string {
  return fromUtc(new Date());
}

/**
 * Streaks over a list of distinct study days (any order).
 * `current` counts consecutive days ending today, or ending yesterday when nothing has been
 * logged yet today (the streak is still alive until the day is over).
 */
export function streaks(days: readonly string[], today: string): { current: number; longest: number } {
  const sorted = [...new Set(days)].filter((d) => d <= today).sort();
  let longest = 0;
  let run = 0;
  let previous: string | undefined;
  for (const day of sorted) {
    run = previous !== undefined && addDays(previous, 1) === day ? run + 1 : 1;
    longest = Math.max(longest, run);
    previous = day;
  }

  const set = new Set(sorted);
  let cursor = set.has(today) ? today : addDays(today, -1);
  let current = 0;
  while (set.has(cursor)) {
    current += 1;
    cursor = addDays(cursor, -1);
  }
  return { current, longest };
}
