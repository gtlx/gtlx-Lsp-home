package com.gtlx.launchertweaks.config

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle

/**
 * 配置 ContentProvider —— 无 root 跨进程共享配置
 *
 * 目标进程（Launcher）通过 ContentResolver 读写配置。
 * ContentObserver 监听 URI 变化实现热更新。
 */
class ConfigProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.gtlx.launchertweaks.configprovider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/config")

        const val METHOD_GET_CONFIG = "get_config"
        const val METHOD_SET_CONFIG = "set_config"

        const val KEY_ENABLE_180 = "enable_180_rotation"
        const val KEY_ENABLE_AUTO = "enable_auto_rotate"
    }

    override fun onCreate(): Boolean {
        // ★ 修复：App 进程重启（熄屏/内存回收/杀后台）后 Provider 内存=默认值，
        // 必须从磁盘文件重载，否则 Launcher 读到默认配置，表现为"配置还原"。
        try {
            val ctx = context ?: return true
            TweakConfig.loadFromFile(ctx)
        } catch (_: Throwable) {}
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        val context = context ?: return MatrixCursor(arrayOf())
        val cursor = MatrixCursor(arrayOf(KEY_ENABLE_180, KEY_ENABLE_AUTO))
        cursor.addRow(arrayOf(
            if (TweakConfig.enable180Rotation) 1 else 0,
            if (TweakConfig.enableAutoRotate) 1 else 0
        ))
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        return when (method) {
            METHOD_GET_CONFIG -> {
                Bundle().apply {
                    putBoolean(KEY_ENABLE_180, TweakConfig.enable180Rotation)
                    putBoolean(KEY_ENABLE_AUTO, TweakConfig.enableAutoRotate)
                }
            }
            METHOD_SET_CONFIG -> {
                if (extras != null) {
                    val e180 = extras.getBoolean(KEY_ENABLE_180, true)
                    val eAuto = extras.getBoolean(KEY_ENABLE_AUTO, true)
                    TweakConfig.saveFromProvider(context, e180, eAuto)
                    context.contentResolver.notifyChange(CONTENT_URI, null)
                }
                Bundle().apply { putBoolean("success", true) }
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.$AUTHORITY.config"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
