package com.gtlx.launchertweaks.feature

import android.content.Context
import com.gtlx.launchertweaks.config.TweakConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 功能管理器 —— 注册/初始化所有功能模块
 *
 * 新增功能步骤：
 * 1. feature/ 下新建一个 XxxFeature.kt
 * 2. 在 initAll() 里调用 XxxFeature.init(lpparam, context)
 * 3. 在 onConfigChanged() 里调用 XxxFeature.applyConfig()
 */
object FeatureManager {
    private var featuresInitialized = false
    private var appContext: Context? = null

    fun initAll(lpparam: XC_LoadPackage.LoadPackageParam, context: Context) {
        if (featuresInitialized) return
        appContext = context
        TweakConfig.log("initializing all features...")

        // 加载配置
        TweakConfig.load(context)

        // 初始化各功能（按需加）
        Rotation180Feature.init(lpparam, context)

        featuresInitialized = true
        TweakConfig.log("all features initialized OK")
    }

    fun onConfigChanged() {
        if (!featuresInitialized) return
        TweakConfig.log("config changed, reapplying features...")

        // 各功能重新应用配置
        Rotation180Feature.applyConfig()
    }
}
