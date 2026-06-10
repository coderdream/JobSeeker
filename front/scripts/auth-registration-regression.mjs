import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../app/login/page.tsx", import.meta.url), "utf8");

assert.match(source, /注册/, "Expected the auth page to expose a registration mode.");
assert.match(source, /confirmPassword/, "Expected the auth page to include confirmPassword handling.");
assert.match(source, /注册成功，请使用新账号登录/, "Expected the auth page to guide the user back to manual login after registration.");

console.log("Auth registration regression passed.");
