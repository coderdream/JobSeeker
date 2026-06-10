import assert from "node:assert"
import { readFileSync } from "node:fs"

const apiClient = readFileSync("lib/api-client.ts", "utf8")
const authedRequest = readFileSync("components/auth/useAuthedRequest.ts", "utf8")
const envConfigPage = readFileSync("app/env-config/page.tsx", "utf8")
const aiConfigPage = readFileSync("app/ai-config/page.tsx", "utf8")

assert.match(apiClient, /status\s*===\s*401\s*\|\|\s*status\s*===\s*403/, "apiFetch should treat 401 and 403 as auth failures.")
assert.match(apiClient, /export function isApiAuthError/, "api-client should expose a shared auth-error predicate for UI handlers.")
assert.match(apiClient, /isApiAuthError\(error: unknown\)/, "The shared auth-error predicate should accept unknown caught errors.")
assert.match(apiClient, /readErrorMessage\(response\)/, "apiFetchJson should read backend error messages for non-2xx responses.")
assert.match(apiClient, /if\s*\(!response\.ok\)/, "apiFetchJson should reject non-2xx JSON responses.")
assert.match(authedRequest, /isApiAuthError\(error\)/, "useAuthedRequest should use the shared auth-error predicate.")
assert.match(authedRequest, /await logout\(\)/, "useAuthedRequest should clear auth state after auth failure.")
assert.match(envConfigPage, /isApiAuthError/, "Env config should suppress expected auth-expiry errors during redirect.")
assert.match(aiConfigPage, /isApiAuthError/, "AI config should suppress expected auth-expiry errors during redirect.")

console.log("API client auth regression passed.")
