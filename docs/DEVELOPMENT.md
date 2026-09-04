# 开发手册

## 项目结构

```
app/src/main/java/com/gtlx/launchertweaks/
├── MainHook.kt            # Xposed 入口（handleLoadPackage）
├── config/
│   └── TweakConfig.kt     # 配置读写 + 热更新（FileObserver）
├── feature/
│   ├── FeatureManager.kt  # 功能管理器（注册/初始化/配置变更分发）
│   └── Rotation180Feature.kt  # 180°旋转解锁
└── ui/
    └── MainActivity.kt    # 设置界面
```

## 环境准备

编译走 `ssh arch` 虚拟机，环境同状态栏漂流瓶模块。
详见 lsposed-module-dev 技能。

## 新增功能步骤

1. `feature/` 下新建 `XxxFeature.kt`
2. 实现 `init(lpparam, context)` —— 做 hook
3. 实现 `applyConfig()` —— 配置热更新时调用
4. 在 `FeatureManager.initAll()` 里注册
5. 在 `FeatureManager.onConfigChanged()` 里加调用
6. 在 `TweakConfig` 里加配置项
7. 在 `MainActivity` 里加设置控件

## 配置同步

App 端 → 桌面进程 之间的配置传递，用「root 写 `/data/local/tmp/` + FileObserver」方案。
详见 lsposed-module-dev 技能的「配置同步方案」。

## 调试

```bash
# 看桌面日志
adb shell logcat -s LauncherTweaks

# 重启桌面
adb shell su -c "am force-stop com.android.launcher3"

# 看模块是否加载
adb shell su -c '/data/adb/lspd/cli modules ls'
```

## 支持的桌面

- LineageOS Trebuchet (`com.android.launcher3`)
- AOSP Launcher3 (`com.android.launcher3`)
- Pixel Launcher (`com.google.android.apps.nexuslauncher`)

MIUI/ColorOS 等第三方桌面需要适配（类名不一样）。

## 常见坑

| 坑 | 解法 |
|---|---|
| 配置改了桌面不生效 | 桌面 Activity 已经创建，方向已锁定 → 重启桌面 |
| 桌面包名不对 | 用 `cmd package resolve-activity -c android.intent.category.HOME` 查 |
| hook 不生效 | 确认作用域包名正确，类名在目标 Launcher 里存在 |
