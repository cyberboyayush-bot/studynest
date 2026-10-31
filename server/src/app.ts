import { type Context, Hono } from "hono";
import { bodyLimit } from "hono/body-limit";
import { getConnInfo } from "hono/bun";
import { HTTPException } from "hono/http-exception";
import { logger } from "hono/logger";
import { secureHeaders } from "hono/secure-headers";
import { auth, CLIENT_IP_HEADER } from "./auth";
import { env } from "./env";
import { apiError, type AppEnv } from "./http";
import { requireSession } from "./middleware";
import { assignmentRoutes } from "./routes/assignments";
import { focusRoutes } from "./routes/focus";
import { meRoutes } from "./routes/me";
import { subjectRoutes } from "./routes/subjects";

export const app = new Hono<AppEnv>();

/** The real socket address of the caller, or undefined when not served by Bun (e.g. in tests). */
function clientIp(c: Context): string | undefined {
  try {
    return getConnInfo(c).remote.address;
  } catch {
    return undefined;
  }
}

if (env.NODE_ENV !== "test") app.use(logger());

app.use(secureHeaders());
app.use(
  bodyLimit({
    maxSize: 16 * 1024,
    onError: () => {
      throw apiError(413, "payload_too_large", "Request body is too large");
    },
  }),
);

app.get("/health", (c) => c.json({ ok: true }));

app.on(["GET", "POST"], "/api/auth/*", (c) => {
  const headers = new Headers(c.req.raw.headers);
  headers.delete(CLIENT_IP_HEADER);
  const ip = clientIp(c);
  if (ip) headers.set(CLIENT_IP_HEADER, ip);
  return auth.handler(new Request(c.req.raw, { headers }));
});

app.use("/api/*", requireSession);
app.route("/api", subjectRoutes);
app.route("/api", assignmentRoutes);
app.route("/api", focusRoutes);
app.route("/api", meRoutes);

app.notFound(() => {
  throw apiError(404, "not_found", "Route not found");
});

app.onError((error, c) => {
  if (error instanceof HTTPException) {
    if (error.res) return error.getResponse();
    const status = error.status;
    const code = status === 401 ? "unauthorized" : status === 404 ? "not_found" : "bad_request";
    return c.json({ error: { code, message: error.message || "Invalid request" } }, status);
  }
  console.error(error);
  return c.json({ error: { code: "internal_error", message: "Something went wrong" } }, 500);
});
