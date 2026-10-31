import { betterAuth } from "better-auth";
import { drizzleAdapter } from "better-auth/adapters/drizzle";
import { APIError } from "better-auth/api";
import { bearer } from "better-auth/plugins";
import { DEFAULT_DAILY_GOAL_MINUTES } from "./constants";
import { db } from "./db/client";
import * as schema from "./db/schema";
import { env } from "./env";

/** Set only by our own Hono handler from the socket address; any client-supplied value is dropped. */
export const CLIENT_IP_HEADER = "x-studynest-client-ip";

export const auth = betterAuth({
  appName: "StudyNest",
  baseURL: env.BETTER_AUTH_URL,
  secret: env.BETTER_AUTH_SECRET,
  database: drizzleAdapter(db, { provider: "pg", schema }),
  emailAndPassword: {
    enabled: true,
    minPasswordLength: 8,
    maxPasswordLength: 128,
    autoSignIn: true,
  },
  user: {
    additionalFields: {
      dailyFocusGoalMinutes: {
        type: "number",
        required: false,
        defaultValue: DEFAULT_DAILY_GOAL_MINUTES,
        input: false,
      },
    },
  },
  session: {
    expiresIn: 60 * 60 * 24 * 30,
    updateAge: 60 * 60 * 24,
  },
  rateLimit: {
    enabled: env.RATE_LIMIT_ENABLED,
    window: 60,
    max: 100,
    customRules: {
      "/sign-in/email": { window: 60, max: 10 },
      "/sign-up/email": { window: 60, max: 5 },
    },
  },
  advanced: {
    database: { generateId: () => crypto.randomUUID() },
    // Only trust the header our own handler sets from the socket address (see app.ts),
    // so clients can't dodge rate limits with a forged X-Forwarded-For.
    ipAddress: { ipAddressHeaders: [CLIENT_IP_HEADER] },
  },
  databaseHooks: {
    user: {
      create: {
        before: async (data) => {
          const name = typeof data.name === "string" ? data.name.trim() : "";
          if (name.length === 0 || name.length > 60) {
            throw new APIError("BAD_REQUEST", { message: "Name must be between 1 and 60 characters" });
          }
          return { data: { ...data, name } };
        },
      },
    },
  },
  logger: { disabled: env.NODE_ENV === "test" },
  plugins: [bearer({ requireSignature: true })],
});

export type AuthSession = typeof auth.$Infer.Session;
