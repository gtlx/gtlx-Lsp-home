package com.gtlx.launchertweaks.config

import android.content.Context
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * 配置管理 —— App 端写，SystemUI/Launcher 端读
 *
 * 配置同步方案：root + /data/local/tmp/（详见 lsposed-module-dev 技能）
 * 优先读外部存储，其次 /data/local/tmp/，最后内部私有目录兜底
 */
object TweakConfig {
    private const val TAG = "LauncherTweaks"
    private const val CONFIG_FILE = "launcher_tweaks.conf"

    // 开关项
    var enable180Rotation = true   // 桌面 180° 旋转
    var enableAutoRotate = true    // 桌面自动旋转总开关（如果 Launcher 锁了方向）

    private var fileObserver: FileObserver? = null
    private var configFile: File? = null

    fun log(msg: String) {
        Log.i(TAG, msg)
        try { XposedBridge.log("[$TAG] $msg") } catch (_: Throwable) {}
    }

    fun logE(msg: String, t: Throwable? = null) {
        Log.e(TAG, msg, t)
        try { XposedBridge.log("[$TAG] ERROR: $msg") } catch (_: Throwable) {}
    }

    /** 从配置文件加载（SystemUI/Launcher 端调用） */
    fun load(context: Context) {
        val f = getConfigFile(context)
        if (f == null) {
            log("no config file found, using defaults")
            return
        }
        configFile = f
        try {
            val props = Properties()
            FileInputStream(f).use { props.load(it) }
            enable180Rotation = props.getProperty("enable_180_rotation", "true")?.toBoolean() ?: true
            enableAutoRotate = props.getProperty("enable_auto_rotate", "true")?.toBoolean() ?: true
            log("config loaded from ${f.absolutePath}: 180_rotation=$enable180Rotation, auto_rotate=$enableAutoRotate")
        } catch (t: Throwable) {
            logE("load config FAILED", t)
        }

        // 启动文件监听，配置变更热更新
        startWatcher(context, f)
    }

    private fun startWatcher(context: Context, file: File) {
        try {
            fileObserver = object : FileObserver(file.parentFile!!, MODIFY or CREATE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == file.name) {
                        log("config file changed, reloading...")
                        load(context)
                        // 通知功能模块重新应用配置
                        com.gtlx.launchertweaks.feature.FeatureManager.onConfigChanged()
                    }
                }
            }
            fileObserver?.startWatching()
            log("config watcher started (${file.absolutePath})")
        } catch (t: Throwable) {
            logE("start watcher FAILED", t)
        }
    }

    private fun getConfigFile(context: Context): File? {
        // 1. 外部存储 App 私有目录
        try {
            val extDir = File(
                Environment.getExternalStorageDirectory(),
                "Android/data/com.gtlx.launchertweaks/files/$CONFIG_FILE"
            )
            if (extDir.exists()) return extDir
        } catch (_: Throwable) {}
        // 2. /data/local/tmp/ 兜底（App 有 root 时会复制一份到这）
        try {
            val tmpFile = File("/data/local/tmp/$CONFIG_FILE")
            if (tmpFile.exists()) return tmpFile
        } catch (_: Throwable) {}
        // 3. App 内部私有目录（只有 root 才能读到）
        try {
            val appDir = File("/data/data/com.gtlx.launchertweaks/files/$CONFIG_FILE")
            if (appDir.exists()) return appDir
        } catch (_: Throwable) {}
        // 4. 从 context 拿
        try {
            val f = File(context.getExternalFilesDir(null), CONFIG_FILE)
            if (f.exists()) return f
        } catch (_: Throwable) {}
        return null
    }

    /** App 端保存配置 */
    fun saveFromUi(context: Context, enable180: Boolean, enableAuto: Boolean) {
        Thread {
            try {
                val extDir = context.getExternalFilesDir(null)
                val dir = extDir ?: context.filesDir
                val file = File(dir, CONFIG_FILE)
                val props = Properties()
                props.setProperty("enable_180_rotation", enable180.toString())
                props.setProperty("enable_auto_rotate", enableAuto.toString())
                java.io.FileOutputStream(file).use { props.store(it, "LauncherTweaks config") }
                file.setReadable(true, false)

                // 用 root 权限复制到 /data/local/tmp/
                try {
                    val proc = Runtime.getRuntime().exec(arrayOf(
                        "su", "-c",
                        "cp ${file.absolutePath} /data/local/tmp/$CONFIG_FILE && chmod 644 /data/local/tmp/$CONFIG_FILE"
                    ))
                    val code = proc.waitFor()
                    log("sync config to /data/local/tmp exitCode=$code")
                } catch (t: Throwable) {
                    logE("sync config to tmp FAILED", t)
                }
                log("config saved: 180=$enable180, auto=$enableAuto, path=${file.absolutePath}")
            } catch (t: Throwable) {
                logE("save config FAILED", t)
            }
        }.start()
    }
}
