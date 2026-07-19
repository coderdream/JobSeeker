const { chromium } = require('playwright');

(async () => {
  console.log("启动无头浏览器...");
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  try {
    console.log("访问页面 http://127.0.0.1:6866/...");
    await page.goto('http://127.0.0.1:6866/', { waitUntil: 'networkidle' });
    
    // 生成随机用户名
    const testUser = 'agent_tester_' + Date.now();
    const testPass = 'playwright123';
    
    console.log("切换到注册标签...");
    await page.getByText('注册', { exact: true }).click();
    
    console.log("填写注册信息...");
    await page.getByPlaceholder('4-32 位字母、数字或下划线').fill(testUser);
    await page.getByPlaceholder('请输入昵称').fill(testUser);
    await page.getByPlaceholder('8-64 位，需包含字母和数字').fill(testPass);
    await page.getByPlaceholder('请再次输入密码').fill(testPass);
    
    console.log("点击注册按钮...");
    // 假设注册按钮的文字是“注册”，需要匹配按钮
    await page.getByRole('button', { name: '创建账号' }).click();
    
    // 等待 2 秒
    await page.waitForTimeout(2000);
    
    console.log("切换到登录标签...");
    await page.getByText('登录', { exact: true }).first().click();
    
    console.log("填写登录信息...");
    await page.getByPlaceholder('请输入用户名').fill(testUser);
    await page.getByPlaceholder('请输入密码').fill(testPass);
    
    console.log("点击登录按钮...");
    await page.getByRole('button', { name: '登录' }).click();
    
    // 等待网络或者跳转
    await page.waitForTimeout(3000);
    
    const title = await page.title();
    console.log("当前页面标题:", title);
    
    const url = page.url();
    console.log("当前页面URL:", url);
    
    if (url.includes('login')) {
      console.log("还在登录页，可能登录失败，截取页面文字...");
      const text = await page.locator('body').innerText();
      console.log("页面内容前200字符:", text.substring(0, 200).replace(/\n/g, ' '));
    } else {
      console.log("登录成功！进入了系统！");
    }
    
  } catch (err) {
    console.error("测试过程出错：", err);
  } finally {
    await browser.close();
  }
})();
