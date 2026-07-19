# Boss 直聘一键投递功能交接文档

## 1. 核心目标
实现“一键投递”功能：在 JobSeeker 网页前端点击【一键投递】后，Java 后端接收到投递请求，利用 Playwright 控制用户本地已登录 Boss 直聘的 Chrome 浏览器，跳转到对应的岗位详情页，并自动点击“立即沟通”按钮。

## 2. 当前架构与机制 (基于 CDP 的 Chrome 控制)
为了绕过常规无头浏览器的强检测，系统目前采用了基于 CDP (Chrome DevTools Protocol) 的外挂模式：
- **独立 Chrome Profile**: 使用 Python 脚本 (`scripts/boss_cdp_raw.py`) 启动一个独立的 Chrome 实例，并开启 CDP 调试端口 `9222`。
- **Playwright 接管**: Java 后端 (`PlaywrightManager.java`) 通过 `Playwright.create().chromium().connectOverCDP("http://127.0.0.1:9222")` 连接到该独立运行的 Chrome。
- **避免封控**: 这种方式避免了 Playwright 常规启动带来的 `webdriver=true` 等显著标记，最大程度模拟了真实用户浏览器的干净环境。

## 3. 遇到的核心难题与表现 (Boss 直聘反爬/风控机制)
在实现“打开详情页 -> 点击立即沟通”这一看似简单的流程时，遭遇了极强的防御机制，主要表现为以下三种情况：

### 问题 A：伪造点击被拦截 (Popup Blocker)
- **尝试**: 通过 Playwright 的 `page.evaluate` 往页面 DOM 中动态注入一个 `<a href="岗位链接" target="_blank">`，并使用 Playwright 的 `locator.click()` 尝试触发原生点击。
- **结果**: 触发了 Chrome 浏览器的 Popup Blocker (弹出窗口拦截器)。浏览器识别到该新窗口并非由用户的真实物理鼠标事件触发，直接将其阻拦，导致 Playwright 抛出 `TargetClosedError`，页面停留在一片空白 (`about:blank`)。

### 问题 B：多标签页漂移与 Target 丢失 (COOP 跨域隔离)
- **尝试**: 使用 Playwright 的 API `page.context().newPage()` 创建一个全新的标签页，然后在这个空标签页中执行 `navigate(jobUrl)`。
- **结果**: Boss 直聘页面很可能配置了 COOP (Cross-Origin-Opener-Policy) 或类似的跨域隔离机制。当命令页面跳转时，Chrome 为了安全强制切断了原有 Target 进程，**弹出了一个新的标签页**来加载岗位详情（用户能在屏幕上看到正确的页面）。但是，Playwright 内部维护的 `detailPage` 对象仍然死锁在最初那个 `about:blank` 标签页中。这导致后端拉取到的页面 HTML 永远是空的 `<html><head></head><body></body></html>`，从而报“未找到立即沟通按钮”的异常。

### 问题 C：页面无限刷新/重定向循环
- **尝试**: 为了规避新标签页带来的 Target 丢失和风控，尝试放弃 `newPage()`，直接在原本已经打开了 Boss 直聘的当前标签页（主页面）上调用 `detailPage.navigate(jobUrl)`。
- **结果**: 触发了 Boss 直聘最严厉的风控。由于跳转来源不是页面内的正常元素点击，Boss 直聘前端脚本或 WAF 将此行为判定为自动化机器人，立刻将页面重定向回首页 `https://www.zhipin.com/`，并陷入不断的检测与无限刷新循环中。

## 4. 尝试过的解决方案汇总
1. **注入 JS a标签并 Playwright 点击**: 失败，被拦截器判定为恶意弹窗。
2. **纯 API 新开标签页并 Navigate**: 失败，COOP/反爬机制导致 Playwright 追踪的 Target 脱离真实展示的页面，拿到空 DOM。
3. **遍历 Context Pages 查找真实页面**: 当检测到 `detailPage` 处于 `about:blank` 时，遍历 `page.context().pages()` 试图抓回被浏览器强制弹出的真实详情页。**最终因为反爬强制重定向，真实页面往往未加载完就被弹回首页**。
4. **复用主标签页直接 Navigate**: 失败，直接触发首页无限重定向与刷新防线。

## 5. 后续 AI 接手建议与突破口
要解决 Boss 直聘这套极端的风控体系，传统的 WebDriver/Playwright URL 跳转 API (`navigate`, `goto`) 已基本失效。接下来的尝试方向必须更偏向于**“纯物理与视觉模拟”**：

1. **避免任何直接的 `navigate` 调用**
   Boss 直聘对 `location.href` 或 `navigate` 指令异常敏感。
   **突破口**：在当前的已登录页面（比如首页）中，使用 Playwright 随机寻找到页面里本来就存在的一个合法岗位链接（`<a>` 标签），利用 `page.evaluate` 悄悄将其 `href` 替换为我们要投递的真实 `jobUrl`。随后，使用 `page.mouse().click(x, y)` 去精确点击该元素的坐标。这样完全伪装成了“用户在首页看到了岗位并点击进入”的真实操作流。
2. **审查 CDP 连接与 Stealth 插件**
   后端直接连接 CDP 时，未加载 `playwright-stealth` 的防护能力。可以考虑在 Python 端通过 `playwright-stealth` 获取页面上下文后，再暴露端口给 Java 端，或者干脆将核心的“点击投递”逻辑用 Python 重写，通过 RPC 与 Java 后端通信。
3. **前端半自动配合模式**
   如果后端全自动破解成本过高，可考虑调整产品形态。在前端提供一个悬浮引导窗或提示，用户在前端点击【一键投递】后，由用户的真实 Chrome 插件（Extension）接管点击操作，而不是完全依赖后端的无头控制。
