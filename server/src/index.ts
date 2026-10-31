import { app } from "./app";
import { env } from "./env";

console.log(`StudyNest API listening on http://${env.HOST}:${env.PORT}`);

export default {
  port: env.PORT,
  hostname: env.HOST,
  fetch: app.fetch,
};
