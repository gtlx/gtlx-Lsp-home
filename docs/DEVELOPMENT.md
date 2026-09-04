# 开发手册

## 项目定位

LSPosed / Vector 模块，针对系统桌面（Launcher3 系）做定制优化。
当前版本：v0.1.0（0.1.x 起步，功能演进加 minor，修复加 patch）

## 项目结构

```
app/src/main/java/com/gtlx/launchertweaks/
├── MainHook.kt            # Xposed 入口（handleLoadPackage）
├── config/
│   ├── TweakConfig.kt     # 配置读写（ContentProvider 跨进程，无 root）
│   └── ConfigProvider.kt  # ContentProvider，目标进程读配置的入口
├── feature/
│   ├── FeatureManager.kt  # 功能管理器（注册/初始化/配置变更分发）
│   └── Rotation180Feature.kt  # 180°旋转解锁
└── ui/
    └── MainActivity.kt    # 设置界面（Soft UI + Glassmorphism）
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

ContentProvider 方案（无 root）：目标进程（Launcher）通过 ContentResolver
读模块 App 的 Provider，ContentObserver 监听热更新。
详见 lsposed-module-dev 技能的「配置同步方案」。

## 调试

```bash
# 看桌面日志
adb shell logcat -s LauncherTweaks

# 重启桌面
adb shell su -c "am force-stop com.android.launcher3"

# 看模块是否加载
adb shell su -c '/data/adb/lspd/cli modules ls'

# 直接调 Provider 测试
adb shell content call --uri content://com.gtlx.launchertweaks.configprovider --method get_config
```

## 🚀 路线图（分期）

> 版本规则：新项目 0.1.x 起步（语义化版本，功能演进加 minor，修复加 patch）
> 规划按「期」组织，一期对应一个交付里程碑，动手前先与用户确认范围。

### 一期 ✅（首发可用，v0.1.0）
- [x] 180° 反向竖屏解锁
- [x] 设置界面（Soft UI + Glassmorphism）
- [x] 配置同步 ContentProvider 化（无 root）
- [x] root 增强保留（一键重启桌面）

### 二期（动效优化，未开工）
- [ ] **动效调优**（见下方专项规划）

### 三期（手势与布局增强，未开工）
- [ ] 双击桌面空白锁屏 / 桌面下拉手势
- [ ] 图标大小/网格行列自定义

### 四期（进阶，未开工）
- [ ] 文件夹增强（展开动画/智能建议）
- [ ] App 抽屉增强（分类/最近使用）

### 远期设想
- [ ] 图标堆叠（自动整理同类 app）
- [ ] Widget 堆叠（智能叠放，较复杂）
- [ ] 桌面壁纸视差/滚动效果

### ✨ 动效专项（讨论稿，未开工）

**背景**：桌面有时候会出现"莫名其妙的动效问题"（动画卡顿/突兀/不自然），后续主攻方向是把动效打磨顺滑自然。

**方向**：
- 排查现有动效异常（先定位"莫名其妙"的具体场景：翻页/开文件夹/回桌面/启动 app？）
- 桌面翻页动画调优（跟手性、惯性、回弹）
- 文件夹开合动画自然化
- 图标进入/退出动画
- 整体过渡一致性（遵循系统动画插值器与时长规范）

**注意**：动效类改动用户偏好"先讨论方案再动手"，且验收要真实环境效果而非只看日志。
先收集具体问题场景（哪些操作出现什么异常），再针对性修。

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
| 更新 APK 后跑的还是旧代码 | arch 上编译要完整同步新文件（scp 漏文件导致旧代码），用 rm -rf + scp -r 全量同步 |
