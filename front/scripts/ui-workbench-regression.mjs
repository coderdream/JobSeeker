import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"

const read = (path) => readFileSync(path, "utf8")

const requiredSharedComponents = [
  "components/workbench/feedback-dialog.tsx",
  "components/workbench/platform-status-bar.tsx",
  "components/workbench/form-field.tsx",
  "components/workbench/status-pill.tsx",
  "components/workbench/analysis-workbench.tsx",
  "components/workbench/jobs-data-table.tsx",
]

for (const file of requiredSharedComponents) {
  assert.ok(existsSync(file), `Expected shared workbench component ${file} to exist.`)
}

const homePage = read("app/page.tsx")
const loginPage = read("app/login/page.tsx")
const envPage = read("app/env-config/page.tsx")
const aiPage = read("app/ai-config/page.tsx")
const baseDataPage = read("app/base-data/page.tsx")
const bossPage = read("app/boss/page.tsx")
const job51Page = read("app/51job/page.tsx")
const liepinPage = read("app/liepin/page.tsx")
const zhilianPage = read("app/zhilian/page.tsx")
const yupaoPage = read("app/yupao/page.tsx")
const contentArea = read("app/components/ContentArea.tsx")

assert.match(homePage, /正在进入工作台|工作台加载中/, "Expected home redirect to render a visible loading state.")
assert.match(loginPage, /showLoginPassword/, "Expected login form to support password visibility toggling.")
assert.match(loginPage, /showRegisterPassword/, "Expected registration form to support password visibility toggling.")
assert.match(loginPage, /招聘自动化工作台/, "Expected login page to explain the product context.")

for (const [name, source] of [["env", envPage], ["ai", aiPage]]) {
  assert.doesNotMatch(source, /alert\(/, `Expected ${name} config page not to use browser alert feedback.`)
  assert.match(source, /FeedbackDialog/, `Expected ${name} config page to use the shared FeedbackDialog.`)
}

assert.match(envPage, /后端配置存储/, "Expected environment config copy to describe backend configuration storage instead of .env files.")
assert.match(aiPage, /Boss 自动投递 AI 开关/, "Expected AI page to label the Boss-scoped AI enable switch explicitly.")

assert.match(baseDataPage, /ConfirmDialog/, "Expected base data deletes to use a confirmation dialog.")
assert.match(baseDataPage, /validate/, "Expected base data forms to validate required values before saving.")

for (const [name, source] of [
  ["boss", bossPage],
  ["51job", job51Page],
  ["liepin", liepinPage],
  ["zhilian", zhilianPage],
  ["yupao", yupaoPage],
]) {
  assert.match(source, /PlatformStatusBar/, `Expected ${name} config page to use the shared PlatformStatusBar.`)
  assert.match(source, /FeedbackDialog/, `Expected ${name} config page to use the shared FeedbackDialog.`)
}

assert.match(liepinPage, /accentBgClass="bg-orange-500"/, "Expected Liepin header accent to be orange.")
assert.match(job51Page, /accentBgClass="bg-amber-500"/, "Expected 51job header accent to be amber.")
assert.match(zhilianPage, /accentBgClass="bg-sky-500"/, "Expected Zhilian header accent to be sky.")
assert.match(yupaoPage, /试验平台|Beta/, "Expected Yupao page to be clearly marked as a beta platform.")

assert.match(contentArea, /pathname\.startsWith\('\/boss'\)/, "Expected platform accent matching to include nested Boss routes.")
assert.match(contentArea, /pathname\.startsWith\('\/51job'\)/, "Expected platform accent matching to include nested 51job routes.")
assert.match(contentArea, /pathname\.startsWith\('\/liepin'\)/, "Expected platform accent matching to include nested Liepin routes.")
assert.match(contentArea, /pathname\.startsWith\('\/zhilian'\)/, "Expected platform accent matching to include nested Zhilian routes.")
assert.match(contentArea, /pathname\.startsWith\('\/yupao'\)/, "Expected platform accent matching to include nested Yupao routes.")
assert.match(contentArea, /accent-emerald/, "Expected Yupao content area to use the emerald platform accent.")

console.log("UI workbench regression passed.")
