# Design: 测试基建固化与代码规范加固

## Decisions
1. **puppeteer 归属**：与参考工程一致放 frontend-web devDependencies（24.10.0，PUPPETEER_SKIP_DOWNLOAD 安装，运行时用本机 Edge executablePath）；tests/e2e 通过 createRequire 以相对路径解析，保证自包含。
2. **vitest 双端同版本 3.2.4**：h5 测试 stub localStorage（node 环境无 DOM）；web 测纯函数（fen2yuan/yuan2fen/unwrapPage），不引 jsdom 控制依赖体积。
3. **Web UI E2E 策略**：history 路由直接 path 访问；token 键 zl_admin_token；登录页用户名默认预填 admin，脚本先清空再输入（回归发现：直接叠加输入会变成 adminadmin 导致 401）；导航 goto 带 domcontentloaded + 重试（Vite 按需编译偶发卡顿）。
4. **lint 配置**：直接复用参考工程 flat config（web 全量；h5 去除 @typescript-eslint 依赖，纯 JS 规则集），`no-console` off（移动端调试需要）。
5. **后端修复**：越层下沉到 Service 并顺手补业务校验（在售商品拒绝登记到货提醒）；批量失败原因仅透 ServiceException。

## Risks
- Edge executablePath 硬编码路径，跨机器需环境变量化（列入后续建议）。
- dev server 按需编译导致首轮 UI E2E 较慢（~9 min），CI 上建议先 `vite build` + preview。
