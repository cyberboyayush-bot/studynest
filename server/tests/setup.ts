import { afterAll, beforeAll } from "bun:test";
import { migrate } from "drizzle-orm/postgres-js/migrator";
import { db, sql } from "../src/db/client";

if (!process.env.DATABASE_URL?.endsWith("_test")) {
  throw new Error("Refusing to run tests against a non-test database");
}

beforeAll(async () => {
  await migrate(db, { migrationsFolder: new URL("../drizzle", import.meta.url).pathname });
  await sql`TRUNCATE "focus_session", "assignment", "subject", "session", "account", "verification", "user" CASCADE`;
});

afterAll(async () => {
  await sql.end();
});
