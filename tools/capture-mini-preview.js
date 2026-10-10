const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright");

const projectRoot = "F:\\x项目文档\\j未保小程序\\miniprogram\\miniprogram";
const outDir = path.resolve(__dirname, "..", "deploy_tmp");
const htmlPath = path.join(outDir, "mini-home-preview.html");
const screenshotPath = path.resolve(__dirname, "..", "未保小程序首页预览.png");

function imageData(name) {
  const filePath = path.join(projectRoot, "images", name);
  const ext = path.extname(name).slice(1);
  const data = fs.readFileSync(filePath).toString("base64");
  return `data:image/${ext};base64,${data}`;
}

fs.mkdirSync(outDir, { recursive: true });

const logo = imageData("logo.png");
const media = imageData("media.png");
const homeActive = imageData("home-active.png");
const my = imageData("my.png");

const html = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>检察院未保小程序 Web 原型</title>
  <style>
    * { box-sizing: border-box; }
    html, body { margin: 0; width: 100%; min-height: 100%; background: #eef1f4; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif; }
    body { display: flex; justify-content: center; padding: 22px 0; color: #333; }
    .phone { width: 375px; height: 812px; background: #fff; overflow: hidden; box-shadow: 0 16px 45px rgba(18, 34, 66, .22); border-radius: 30px; position: relative; }
    .status { height: 34px; display: flex; align-items: end; justify-content: space-between; padding: 0 24px 6px; font-size: 12px; font-weight: 600; }
    .capsule { position: absolute; top: 46px; right: 14px; width: 86px; height: 30px; border: 1px solid rgba(0,0,0,.14); border-radius: 18px; display: flex; align-items: center; justify-content: space-around; background: rgba(255,255,255,.74); z-index: 3; }
    .dot { width: 4px; height: 4px; border-radius: 50%; background: #111; box-shadow: 9px 0 #111, 18px 0 #111; transform: translateX(-8px); }
    .ring { width: 16px; height: 16px; border: 2px solid #111; border-radius: 50%; }
    .app { height: calc(100% - 34px); display: flex; flex-direction: column; background: #fff; }
    .header-text { text-align: center; font-size: 18px; line-height: 28px; font-weight: 700; padding: 10px 72px 10px 16px; border-bottom: 1px solid #eee; background: #fff; }
    .header-banner { height: 100px; background: #f8f8f8; border-bottom: 1px solid #ddd; display: flex; align-items: center; justify-content: center; }
    .banner-content { display: flex; align-items: center; justify-content: center; padding: 0 16px; }
    .banner-logo { width: 70px; height: 70px; object-fit: contain; margin-right: 10px; }
    .banner-title-main { display: block; font-size: 20px; line-height: 1.2; font-weight: 700; color: #333; letter-spacing: 1px; white-space: nowrap; }
    .banner-title-sub { display: block; font-size: 12px; color: #666; margin-top: 4px; }
    .content { flex: 1; display: flex; flex-direction: column; min-height: 0; }
    .grid-section { display: grid; grid-template-columns: 1fr 1fr; gap: 2px; background: #b0c4de; }
    .grid-item { height: 100px; background: #ffe4e1; display: flex; align-items: center; justify-content: center; padding: 10px; font-size: 15px; line-height: 1.35; color: #333; }
    .icon-box { font-size: 40px; line-height: 1; margin-right: 8px; }
    .multimedia-section { flex: 1; min-height: 150px; background: #a7ffeb; display: flex; align-items: center; justify-content: center; margin-top: 2px; }
    .media-content { display: flex; align-items: center; font-size: 30px; color: #00695c; font-weight: 700; }
    .media-icon { width: 60px; height: 60px; object-fit: contain; margin-right: 10px; }
    .tabbar { height: 64px; border-top: 1px solid #e6e6e6; display: grid; grid-template-columns: 1fr 1fr; background: #fff; }
    .tab { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; font-size: 11px; color: #7a7a7a; }
    .tab.active { color: #1989fa; }
    .tab img { width: 24px; height: 24px; object-fit: contain; }
  </style>
</head>
<body>
  <main class="phone">
    <div class="status"><span>9:41</span><span>5G&nbsp;&nbsp;100%</span></div>
    <div class="capsule"><span class="dot"></span><span class="ring"></span></div>
    <section class="app">
      <header>
        <div class="header-text">东莞市人民检察院未保强制报告</div>
        <div class="header-banner">
          <div class="banner-content">
            <img class="banner-logo" src="${logo}" alt="">
            <div>
              <span class="banner-title-main">东莞市人民检察院</span>
              <span class="banner-title-sub">www.dongguan.jcy.gov.cn</span>
            </div>
          </div>
        </div>
      </header>
      <section class="content">
        <div class="grid-section">
          <div class="grid-item"><span class="icon-box">📋</span><span>强制专项<br>报告</span></div>
          <div class="grid-item"><span class="icon-box">🔍</span><span>线索举报</span></div>
          <div class="grid-item"><span class="icon-box">⚖️</span><span>法律法规</span></div>
          <div class="grid-item"><span class="icon-box">📖</span><span>案例解读</span></div>
        </div>
        <div class="multimedia-section">
          <div class="media-content"><img class="media-icon" src="${media}" alt=""><span>多媒体课堂</span></div>
        </div>
      </section>
      <nav class="tabbar">
        <div class="tab active"><img src="${homeActive}" alt=""><span>首页</span></div>
        <div class="tab"><img src="${my}" alt=""><span>我的举报</span></div>
      </nav>
    </section>
  </main>
</body>
</html>`;

fs.writeFileSync(htmlPath, html, "utf8");

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 420, height: 860 }, deviceScaleFactor: 2 });
  await page.goto(`file://${htmlPath.replace(/\\/g, "/")}`);
  await page.screenshot({ path: screenshotPath, fullPage: true });
  await browser.close();
  console.log(screenshotPath);
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
