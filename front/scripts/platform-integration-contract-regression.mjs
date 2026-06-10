import assert from "node:assert"
import { existsSync, readFileSync } from "node:fs"

const platformRequests = readFileSync("lib/platform-requests.ts", "utf8")
const bossPage = readFileSync("app/boss/page.tsx", "utf8")
const job51Page = readFileSync("app/51job/page.tsx", "utf8")
const liepinPage = readFileSync("app/liepin/page.tsx", "utf8")
const zhilianPage = readFileSync("app/zhilian/page.tsx", "utf8")
const yupaoPage = readFileSync("app/yupao/page.tsx", "utf8")
const baseDataPage = readFileSync("app/base-data/page.tsx", "utf8")
const selectComponent = readFileSync("components/ui/select.tsx", "utf8")
const job51Analysis = readFileSync("app/51job/analysis/AnalysisContent.tsx", "utf8")
const liepinAnalysis = readFileSync("app/liepin/analysis/AnalysisContent.tsx", "utf8")
const yupaoAnalysis = readFileSync("app/yupao/analysis/AnalysisContent.tsx", "utf8")
assert.ok(existsSync("components/ui/combobox.tsx"), "Expected a shared Combobox component for searchable/custom platform filters.")
const comboboxComponent = readFileSync("components/ui/combobox.tsx", "utf8")

assert.match(selectComponent, /window\.innerWidth/, "Expected shared Select dropdown positioning to account for viewport width.")
assert.match(selectComponent, /maxWidth/, "Expected shared Select dropdown panel to clamp to the viewport.")
assert.match(selectComponent, /placement/, "Expected shared Select dropdown positioning to support opening above the trigger.")
assert.match(selectComponent, /topPanelMaxHeight/, "Expected shared Select dropdowns opened above the trigger to use a compact height.")

assert.match(comboboxComponent, /allowCustom\?:\s*boolean/, "Expected Combobox to expose allowCustom for manual platform codes.")
assert.match(comboboxComponent, /No options|无匹配|暂无/, "Expected Combobox to expose an empty/options-missing state.")
assert.match(comboboxComponent, /topPanelMaxHeight/, "Expected Combobox dropdowns opened above the trigger to use a compact height.")

assert.doesNotMatch(yupaoPage, /value=\{config\.salary \|\| ''\}[\s\S]*?<option value="">[\s\S]*?\{options\.salary\.map/, "Yupao salary select must not add a duplicate blank option when reference data already provides one.")
assert.doesNotMatch(yupaoPage, /value=\{config\.jobType \|\| ''\}[\s\S]*?<option value="">[\s\S]*?\{options\.jobType\.map/, "Yupao job type select must not add a duplicate blank option when reference data already provides one.")
assert.match(baseDataPage, /\/api\/platform-option-types/, "Base data page must load configurable platform option types from the backend.")
assert.doesNotMatch(baseDataPage, /platformOptionTypeCatalog\s*[:=]/, "Base data page must not use a fixed per-platform option type catalog.")
assert.match(baseDataPage, /label:\s*string/, "Base data page must model editable platform option type labels.")

for (const path of ["/api/boss/status", "/api/51job/status", "/api/liepin/status", "/api/zhilian/status", "/api/yupao/status"]) {
  assert.match(platformRequests, new RegExp(path.replaceAll("/", "\\/")), `Expected platform helper to include ${path}.`)
}

assert.match(platformRequests, /\/api\/51job\/login/, "Expected 51job login trigger path.")
assert.match(platformRequests, /\/api\/boss\/login/, "Expected Boss login trigger path.")
assert.match(platformRequests, /\/api\/liepin\/login/, "Expected Liepin login trigger path.")
assert.match(platformRequests, /\/api\/zhilian\/login/, "Expected Zhilian login trigger path.")
assert.match(platformRequests, /\/api\/yupao\/login/, "Expected Yupao login trigger path.")
assert.match(platformRequests, /export class PlatformActionError/, "Expected structured platform action errors.")
assert.match(platformRequests, /isPlatformAlreadyRunningError/, "Expected helper for already-running platform starts.")

assert.match(bossPage, /\/api\/boss\/stream/, "Expected Boss task stream to be connected after start.")
assert.match(job51Page, /\/api\/51job\/stream/, "Expected 51job task stream to be connected after start.")

for (const platform of ["liepin", "zhilian", "yupao"]) {
  const page = { liepin: liepinPage, zhilian: zhilianPage, yupao: yupaoPage }[platform]
  const label = platform[0].toUpperCase() + platform.slice(1)
  assert.match(page, new RegExp(`getPlatformStatus\\(authedFetch,\\s*["']${platform}["']\\)[\\s\\S]*setInterval`), `Expected ${label} to poll platform status.`)
  assert.match(page, new RegExp(`await openPlatformLogin\\(authedFetch,\\s*["']${platform}["']\\)[\\s\\S]*setLoginPolling\\(true\\)`), `Expected ${label} login polling to start after backend login page opens.`)
  assert.match(page, /isStartingDelivery/, `Expected ${label} to track start-pending state.`)
  assert.match(page, /const refreshDeliveryStatus = useCallback/, `Expected ${label} to have an immediate status refresh helper.`)
  assert.match(page, new RegExp(`setIsStartingDelivery\\(true\\)[\\s\\S]*await startPlatformTask\\(authedFetch,\\s*["']${platform}["']\\)[\\s\\S]*await refreshDeliveryStatus\\(\\)`), `Expected ${label} start success to refresh platform status immediately.`)
  assert.match(page, /isPlatformAlreadyRunningError\(error\)[\s\S]*setIsDelivering\(true\)[\s\S]*await refreshDeliveryStatus\(\)/, `Expected ${label} already-running start response to keep delivery state active.`)
  assert.match(page, new RegExp(`openPlatformLogin\\(authedFetch,\\s*["']${platform}["']\\)`), `Expected ${label} login button to open backend login flow.`)
}

assert.match(liepinPage, /loginPolling[\s\S]*getPlatformStatus\(authedFetch,\s*["']liepin["']\)[\s\S]*setInterval/, "Expected Liepin to poll login status while login check is active.")
for (const field of ["compTag", "pubTime", "workYearCode", "eduLevel", "industry", "jobKind", "compScale", "compStage", "compKind"]) {
  assert.match(liepinPage, new RegExp(`${field}\\?:\\s*string`), `Expected Liepin config to include ${field}.`)
  assert.match(liepinPage, new RegExp(`renderFilterCombobox\\(["']${field}["']`), `Expected Liepin ${field} filter to use the searchable/custom Combobox.`)
}
for (const optionType of ["compTag", "pubTime", "workYearCode", "degree", "industry", "jobType", "scale", "stage", "compKind"]) {
  assert.match(liepinPage, new RegExp(`${optionType}:\\s*LiepinOption\\[]`), `Expected Liepin options to include ${optionType}.`)
}
for (const label of ["名企", "招聘者活跃", "经验", "学历", "行业", "职位类型", "企业规模", "融资阶段", "企业性质"]) {
  assert.match(liepinPage, new RegExp(label), `Expected Liepin page to render ${label} filter.`)
}
assert.match(liepinPage, /allowCustom/, "Expected Liepin filter comboboxes to allow manual platform code entry.")
assert.match(zhilianPage, /loginPolling[\s\S]*getPlatformStatus\(authedFetch,\s*["']zhilian["']\s*,\s*\{\s*refreshLogin:\s*true\s*\}\)[\s\S]*setInterval/, "Expected Zhilian to poll refreshed login status while login check is active.")
assert.match(zhilianPage, /addEventListener\(["']focus["'][\s\S]*refreshDeliveryStatus\(\)/, "Expected Zhilian to refresh real login status when returning to the app.")
assert.match(zhilianPage, /setIsLoggedIn\(\(current\) => current \|\| loggedIn\)/, "Expected Zhilian connected SSE cache to avoid overwriting a refreshed logged-in state.")
assert.match(yupaoPage, /loginPolling[\s\S]*getPlatformStatus\(authedFetch,\s*["']yupao["']\s*,\s*\{\s*refreshLogin:\s*true\s*\}\)[\s\S]*setInterval/, "Expected Yupao to poll refreshed login status while login check is active.")
assert.match(job51Page, /openPlatformLogin\(authedFetch,\s*["']51job["']\)/, "Expected 51job login button to open backend login flow.")
assert.match(bossPage, /openPlatformLogin\(authedFetch,\s*["']boss["']\)/, "Expected Boss login button to open backend login flow.")

for (const status of ["未投递", "已投递", "已过滤", "投递失败"]) {
  assert.match(yupaoAnalysis, new RegExp(status), `Expected Yupao analysis status filter to include ${status}.`)
}

assert.match(job51Analysis, /statusOptions/, "Expected 51job analysis to expose status filters.")
assert.match(liepinAnalysis, /statusOptions/, "Expected Liepin analysis to expose status filters.")

console.log("Platform integration contract regression passed.")
