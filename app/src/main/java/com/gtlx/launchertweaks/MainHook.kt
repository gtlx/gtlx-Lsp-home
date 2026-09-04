package com.gtlx.launchertweaks

import android.content.Context
import com.gtlx.launchertweaks.config.TweakConfig
import com.gtlx.launchertweaks.feature.FeatureManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 只处理桌面（Launcher）进程
        if (!isTargetPackage(lpparam.packageName)) return

        TweakConfig.log("=== target hit: ${lpparam.packageName} ===")

        // 找一个能拿 Context 的时机 —— hook Launcher Activity 的 onCreate
        try {
            val launcherClass = XposedHelpers.findClass(
                "com.android.launcher3.Launcher", lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                launcherClass, "onCreate",
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.thisObject as Context
                        TweakConfig.log("Launcher.onCreate → init features")
                        FeatureManager.initAll(lpparam, context)
                    }
                }
            )
            TweakConfig.log("hooked Launcher.onCreate OK")
        } catch (t: Throwable) {
            TweakConfig.logE("hook Launcher FAILED", t)
        }
    }

    private fun isTargetPackage(pkg: String): Boolean {
        // 支持的桌面：LineageOS Trebuchet / AOSP Launcher3 / Pixel Launcher 等
        val targets = listOf(
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "org.lineageos.trebuchet"
        )
        return pkg in targets
    }
}
