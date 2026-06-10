import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../lib/sse.ts", import.meta.url), "utf8");

assert.match(
  source,
  /Authorization/,
  "Expected SSE helper to send an Authorization header for authenticated streams.",
);

assert.doesNotMatch(
  source,
  /searchParams\.set\(['"]access_token['"]/,
  "Expected SSE helper to stop putting the token in the access_token query param.",
);

console.log("SSE auth regression passed.");
