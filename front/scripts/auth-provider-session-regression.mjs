import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../components/auth/AuthProvider.tsx", import.meta.url), "utf8");

assert.match(
  source,
  /bootstrappedRef/,
  "Expected AuthProvider to run the startup session validation only once instead of re-running after login state changes.",
);

assert.doesNotMatch(
  source,
  /useEffect\(\(\)\s*=>\s*{[\s\S]*?refreshMe\(\)[\s\S]*?},\s*\[refreshMe\]\)/,
  "Expected AuthProvider bootstrap effect not to depend on refreshMe, because refreshMe changes when token changes after login.",
);

assert.match(
  source,
  /isAuthExpired/,
  "Expected AuthProvider to distinguish expired auth from transient /api/auth/me failures.",
);

console.log("Auth provider session regression passed.");
