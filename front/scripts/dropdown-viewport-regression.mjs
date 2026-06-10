import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { join } from 'node:path'

const root = fileURLToPath(new URL('..', import.meta.url))

function read(relativePath) {
  return readFileSync(join(root, relativePath), 'utf8')
}

function assertContains(source, pattern, message) {
  if (!pattern.test(source)) {
    throw new Error(message)
  }
}

function assertNotContains(source, pattern, message) {
  if (pattern.test(source)) {
    throw new Error(message)
  }
}

const sharedSelect = read('components/ui/select.tsx')
const bossPage = read('app/boss/page.tsx')
const job51Page = read('app/51job/page.tsx')
const globals = read('app/globals.css')

assertContains(
  sharedSelect,
  /dropdownPosition[\s\S]*maxHeight/,
  'shared Select must track dropdown maxHeight in positioning state',
)
assertContains(
  sharedSelect,
  /maxHeight:\s*`\$\{dropdownPosition\.maxHeight\}px`/,
  'shared Select must apply dynamic maxHeight to the dropdown panel',
)

assertContains(
  bossPage,
  /dropdownPosition[\s\S]*maxHeight/,
  'Boss MultiSelect must track dropdown maxHeight in positioning state',
)
assertContains(
  bossPage,
  /maxHeight:\s*`\$\{dropdownPosition\.maxHeight\}px`/,
  'Boss MultiSelect must apply dynamic maxHeight to the dropdown panel',
)

assertContains(
  job51Page,
  /salaryDropdownMaxHeight/,
  '51job salary dropdown must track dynamic maxHeight',
)
assertContains(
  job51Page,
  /maxHeight:\s*`\$\{salaryDropdownMaxHeight\}px`/,
  '51job salary dropdown must apply dynamic maxHeight to the dropdown panel',
)

assertNotContains(
  globals,
  /max-h-56/,
  'global dropdown-panel must not force a fixed max height',
)
