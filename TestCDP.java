import com.microsoft.playwright.*;
public class TestCDP {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().connectOverCDP("http://127.0.0.1:9222");
            System.out.println("Connected to CDP!");
            System.out.println("Contexts size: " + browser.contexts().size());
            BrowserContext context = browser.contexts().get(0);
            System.out.println("Pages size: " + context.pages().size());
            System.out.println("Current URL: " + context.pages().get(0).url());
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
