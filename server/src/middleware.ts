import { zValidator } from "@hono/zod-validator";
import { createMiddleware } from "hono/factory";
import type { ValidationTargets } from "hono";
import type { ZodType } from "zod";
import { auth } from "./auth";
import { apiError, type AppEnv } from "./http";

export const requireSession = createMiddleware<AppEnv>(async (c, next) => {
  const result = await auth.api.getSession({ headers: c.req.raw.headers });
  if (!result) throw apiError(401, "unauthorized", "Please sign in to continue");
  c.set("user", result.user);
  c.set("session", result.session);
  await next();
});

export function validate<Target extends keyof ValidationTargets, Schema extends ZodType>(
  target: Target,
  schema: Schema,
) {
  return zValidator(target, schema, (result) => {
    if (!result.success) {
      const issue = result.error.issues[0];
      const field = issue?.path.join(".");
      const message = issue ? (field ? `${field}: ${issue.message}` : issue.message) : "Invalid request";
      throw apiError(400, "bad_request", message);
    }
  });
}
