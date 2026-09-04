package com.gtlx.launchertweaks.config

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * 配置管理 —— ContentProvider 跨进程共享（无 root 标准方案）
 *
 * App 端写本地文件 + 更新内存 + notifyChange；
 * 目标进程通过 ContentResolver.call() 读配置，ContentObserver 监听热更新。
 */
object TweakConfig {
    private const val TAG = "LauncherTweaks"
    private const val CONFIG_FILE = "launcher_tweaks.conf"
    private const val KEY_180 = "enable_180_rotation"
    private const val KEY_AUTO = "enable_auto_rotate"

    var enable180Rotation = true
    var enableAutoRotate = true

    private var observer: ConfigContentObserver? = null
    private var listeners = mutableListOf<() -> Unit>()

    // ===== 目标进程（Launcher）端 =====

    fun loadAndWatchFromProvider(context: Context, onChanged: () -> Unit) {
        loadFromProvider(context)
        listeners.add(onChanged)
        if (observer == null) {
            observer = ConfigContentObserver(Handler(Looper.getMainLooper())) {
                loadFromProvider(context)
                listeners.forEach { it() }
            }
            context.contentResolver.registerContentObserver(
                ConfigProvider.CONTENT_URI, true, observer!!
            )
            log("config ContentObserver registered")
        }
    }

    private fun loadFromProvider(context: Context) {
        try {
            val result = context.contentResolver.call(
                ConfigProvider.CONTENT_URI,
                ConfigProvider.METHOD_GET_CONFIG,
                null, null
            )
            if (result != null) {
                enable180Rotation = result.getBoolean(ConfigProvider.KEY_ENABLE_180, true)
                enableAutoRotate = result.getBoolean(ConfigProvider.KEY_ENABLE_AUTO, true)
                log("config loaded from provider: 180=$enable180Rotation, auto=$enableAutoRotate")
                return
            }
        } catch (t: Throwable) {
            log("loadFromProvider failed: ${t.message}")
        }
        log("fallback to defaults")
    }

    // ===== App 端 =====

    fun saveFromUi(context: Context, enable180: Boolean, enableAuto: Boolean) {
        // 1. 写本地文件
        Thread {
            try {
                val file = File(context.filesDir, CONFIG_FILE)
                val props = Properties()
                props.setProperty(KEY_180, enable180.toString())
                props.setProperty(KEY_AUTO, enableAuto.toString())
                FileOutputStream(file).use { props.store(it, "LauncherTweaks config") }
                file.setReadable(true, false)
                log("config saved to file: 180=$enable180, auto=$enableAuto")
            } catch (t: Throwable) {
                logE("save config FAILED", t)
            }
        }.start()

        // 2. 更新内存值
        enable180Rotation = enable180
        enableAutoRotate = enableAuto

        // 3. 通知 ContentObserver
        try {
            context.contentResolver.notifyChange(ConfigProvider.CONTENT_URI, null)
        } catch (_: Throwable) {}
    }

    fun saveFromProvider(context: Context, enable180: Boolean, enableAuto: Boolean) {
        saveFromUi(context, enable180, enableAuto)
    }

    fun loadFromFile(context: Context) {
        try {
            val file = File(context.filesDir, CONFIG_FILE)
            if (!file.exists()) return
            val props = Properties()
            FileInputStream(file).use { props.load(it) }
            enable180Rotation = props.getProperty(KEY_180, "true")?.toBoolean() ?: true
            enableAutoRotate = props.getProperty(KEY_AUTO, "true")?.toBoolean() ?: true
        } catch (_: Throwable) {}
    }

    // ===== 内部 =====

    fun log(msg: String) {
        Log.i(TAG, msg)
        try { XposedBridge.log("[$TAG] $msg") } catch (_: Throwable) {}
    }

    fun logE(msg: String, t: Throwable? = null) {
        Log.e(TAG, msg, t)
        try { XposedBridge.log("[$TAG] ERROR: $msg") } catch (_: Throwable) {}
    }

    private class ConfigContentObserver(
        handler: Handler,
        private val onChange: () -> Unit
    ) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            Log.d(TAG, "config ContentObserver onChange")
            onChange()
        }
    }
}
