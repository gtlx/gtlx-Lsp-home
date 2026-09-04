package com.gtlx.launchertweaks.feature

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import com.gtlx.launchertweaks.config.TweakConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 桌面 180° 旋转解锁
 *
 * Launcher3/Trebuchet 默认只支持 0° 和 90°/270°，不支持倒过来（180°）。
 * 原理：hook Launcher Activity 的 setRequestedOrientation / onConfigurationChanged，
 * 把强制竖屏的调用改掉，允许 180° 反向竖屏。
 *
 * 目标类：
 * - com.android.launcher3.Launcher（主线 Launcher Activity）
 *
 * Hook 策略：
 * 1. setRequestedOrientation —— 拦截强制竖屏调用，改成 UNSPECIFIED 让系统决定
 * 2. onCreate 后确认方向设置
 */
object Rotation180Feature {
    private const val TAG = "Rotation180Feature"
    private var hookInstalled = false

    fun init(lpparam: XC_LoadPackage.LoadPackageParam, context: Context) {
        if (hookInstalled) return

        val launcherClass = try {
            XposedHelpers.findClass("com.android.launcher3.Launcher", lpparam.classLoader)
        } catch (t: Throwable) {
            TweakConfig.logE("Launcher class not found: ${t.message}")
            return
        }

        // Hook setRequestedOrientation —— 拦截强制竖屏
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "setRequestedOrientation",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!TweakConfig.enable180Rotation) return
                        val orientation = param.args[0] as Int
                        // 如果 Launcher 强制设成 PORTRAIT（竖屏），改成 UNSPECIFIED
                        // 这样系统自动旋转就能包含 180°
                        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                            orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT ||
                            orientation == ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT) {
                            TweakConfig.log("[$TAG] intercept setRequestedOrientation: " +
                                    "${orientationName(orientation)} → UNSPECIFIED")
                            param.args[0] = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                }
            )
            TweakConfig.log("[$TAG] hooked Activity.setRequestedOrientation OK")
        } catch (t: Throwable) {
            TweakConfig.logE("[$TAG] hook setRequestedOrientation FAILED", t)
        }

        // Hook Launcher 的 onAttachedToWindow —— 启动时确保方向自由
        try {
            XposedHelpers.findAndHookMethod(
                launcherClass,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!TweakConfig.enable180Rotation) return
                        val activity = param.thisObject as Activity
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        TweakConfig.log("[$TAG] Launcher.onAttachedToWindow → set UNSPECIFIED")
                    }
                }
            )
            TweakConfig.log("[$TAG] hooked Launcher.onAttachedToWindow OK")
        } catch (t: Throwable) {
            TweakConfig.logE("[$TAG] hook onAttachedToWindow FAILED", t)
        }

        hookInstalled = true
        TweakConfig.log("[$TAG] init OK")
    }

    fun applyConfig() {
        // 配置热更新：方向设置变更需要重启 Launcher 才完全生效
        // （因为 Activity 已经创建了，方向已经定了）
        // 这里打个日志提醒
        TweakConfig.log("[$TAG] config updated (180_rotation=${TweakConfig.enable180Rotation}). " +
                "Restart launcher to apply fully.")
    }

    private fun orientationName(orientation: Int): String {
        return when (orientation) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> "UNSPECIFIED"
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> "PORTRAIT"
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> "LANDSCAPE"
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT -> "SENSOR_PORTRAIT"
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> "SENSOR_LANDSCAPE"
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT -> "USER_PORTRAIT"
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE -> "USER_LANDSCAPE"
            else -> "UNKNOWN($orientation)"
        }
    }
}
