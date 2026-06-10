import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../components/auth/useAuthedRequest.ts", import.meta.url), "utf8");

assert.match(
  source,
  /loadSession\(\)\?\.token/,
  "Expected useAuthedRequest to fall back to the stored session token during first-login route transitions.",
);

assert.match(
  source,
  /AUTH_TOKEN_MISSING/,
  "Expected useAuthedRequest to fail locally when no token exists instead of sending an anonymous request.",
);

assert.doesNotMatch(
  source,
  /apiFetch\(path,\s*\{[\s\S]*?token,\s*\}\)/,
  "Expected useAuthedRequest not to pass the possibly empty React state token directly to apiFetch.",
);

console.log("Authed request token regression passed.");
