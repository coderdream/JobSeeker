import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const apiClientSource = await readFile(new URL("../lib/api-client.ts", import.meta.url), "utf8");
const nextConfigSource = await readFile(new URL("../next.config.ts", import.meta.url), "utf8");

assert.match(
  apiClientSource,
  /NEXT_PUBLIC_API_BASE_URL|API_BASE_URL/,
  "Expected api-client to read a public API base URL environment variable.",
);

assert.match(
  apiClientSource,
  /localhost:8889/,
  "Expected api-client fallback to match backend port 8889.",
);

assert.match(
  nextConfigSource,
  /NEXT_PUBLIC_API_BASE_URL/,
  "Expected next.config.ts to expose NEXT_PUBLIC_API_BASE_URL to the browser.",
);

console.log("API base URL regression passed.");
