import { chromium, devices } from "playwright";

const baseUrl = process.env.SCREENSHOT_BASE_URL || "http://127.0.0.1:4173/";
const desktopDarkPath = process.env.OUT_DESKTOP_DARK || "images/screenshot-web-desktop-dark.png";
const mobileDarkPath = process.env.OUT_MOBILE_DARK || "images/screenshot-web-mobile-dark.png";
const desktopLightPath = process.env.OUT_DESKTOP_LIGHT || "images/screenshot-web-desktop-light.png";
const mobileLightPath = process.env.OUT_MOBILE_LIGHT || "images/screenshot-web-mobile-light.png";

const STORAGE_KEY = "TradeBuddy.settings.v1";

function settingsPayload(mode) {
  return `themeStyle=slate\nthemeMode=${mode}`;
}

async function captureDesktop(browser, mode, outputPath) {
  const context = await browser.newContext({
    viewport: { width: 2048, height: 1220 },
    deviceScaleFactor: 2,
  });
  await context.addInitScript(
    ({ key, value }) => {
      window.localStorage.setItem(key, value);
    },
    { key: STORAGE_KEY, value: settingsPayload(mode) },
  );
  const page = await context.newPage();
  await page.goto(baseUrl, { waitUntil: "domcontentloaded" });
  await waitForAppStable(page);
  await page.screenshot({ path: outputPath });
  await context.close();
}

async function captureMobile(browser, mode, outputPath) {
  const context = await browser.newContext({
    ...devices["iPhone 15 Pro"],
  });
  await context.addInitScript(
    ({ key, value }) => {
      window.localStorage.setItem(key, value);
    },
    { key: STORAGE_KEY, value: settingsPayload(mode) },
  );
  const page = await context.newPage();
  await page.goto(baseUrl, { waitUntil: "domcontentloaded" });
  await waitForAppStable(page);
  await page.screenshot({ path: outputPath });
  await context.close();
}

async function waitForAppStable(page) {
  // Best effort only: CI/web renderer differences can make text locators unreliable.
  await page.waitForLoadState("load", { timeout: 120000 }).catch(() => {});
  await page.waitForLoadState("networkidle", { timeout: 120000 }).catch(() => {});

  const candidates = [
    /Tagesübersicht Sonne\/Mond/i,
    /Zeitstrahl/i,
    /Sun\/Moon/i,
    /Astro/i,
    /TradeBuddy/i,
  ];

  for (const pattern of candidates) {
    const found = await page
      .getByText(pattern)
      .first()
      .waitFor({ timeout: 15000 })
      .then(() => true)
      .catch(() => false);
    if (found) break;
  }

  // Final settle time so first-frame/black-frame captures are less likely.
  await page.waitForTimeout(12000);
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  try {
    await captureDesktop(browser, "dark", desktopDarkPath);
    await captureMobile(browser, "dark", mobileDarkPath);
    await captureDesktop(browser, "light", desktopLightPath);
    await captureMobile(browser, "light", mobileLightPath);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
