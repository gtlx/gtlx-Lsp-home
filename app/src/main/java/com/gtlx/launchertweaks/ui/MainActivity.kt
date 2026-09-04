package com.gtlx.launchertweaks.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import com.gtlx.launchertweaks.config.TweakConfig

/**
 * 设置界面 —— 桌面调教
 *
 * 布局：纯代码写（不用 XML 资源），毛玻璃+软UI风格
 */
class MainActivity : Activity() {

    // 配置状态
    private var enable180 = true
    private var enableAuto = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadConfig()
        syncConfigToTmp()

        val sv = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // 标题
        ll.addView(TextView(this).apply {
            text = "🍀 桌面调教"
            textSize = 28f
            setTextColor(Color.parseColor("#10b981"))
            setPadding(0, 0, 0, 16)
        })
        ll.addView(TextView(this).apply {
            text = "v1.0.0 · 让桌面更自由"
            textSize = 14f
            setTextColor(Color.parseColor("#6b7280"))
            setPadding(0, 0, 0, 48)
        })

        // === 180° 旋转开关 ===
        ll.addView(buildSectionTitle("📱 旋转方向"))
        ll.addView(buildSwitchRow(
            "允许 180° 反向竖屏",
            "手机倒过来也能正常用桌面",
            enable180
        ) { isChecked ->
            enable180 = isChecked
            saveConfig()
        })

        // === 重启桌面按钮 ===
        ll.addView(buildSectionTitle("⚡ 操作"))
        ll.addView(Button(this).apply {
            text = "重启桌面（应用设置）"
            setBackgroundColor(Color.parseColor("#10b981"))
            setTextColor(Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener {
                restartLauncher()
            }
        }.also {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            it.layoutParams = lp
        })

        // 说明
        ll.addView(TextView(this).apply {
            text = "\n💡 修改 180° 旋转开关后，点「重启桌面」生效。" +
                    "\n\n🔒 首次使用需授予 Root 权限（同步配置文件用）。"
            textSize = 13f
            setTextColor(Color.parseColor("#9ca3af"))
            setPadding(0, 32, 0, 0)
        })

        sv.addView(ll)
        setContentView(sv)
    }

    private fun buildSectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.parseColor("#374151"))
            setPadding(0, 24, 0, 12)
        }
    }

    private fun buildSwitchRow(
        title: String,
        desc: String,
        checked: Boolean,
        onChecked: (Boolean) -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        textCol.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(Color.parseColor("#1f2937"))
        })
        textCol.addView(TextView(this).apply {
            text = desc
            textSize = 13f
            setTextColor(Color.parseColor("#6b7280"))
            setPadding(0, 4, 0, 0)
        })
        val sw = Switch(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onChecked(isChecked) }
        }
        row.addView(textCol)
        row.addView(sw)
        return row
    }

    private fun loadConfig() {
        try {
            val dir = getExternalFilesDir(null) ?: filesDir
            val file = java.io.File(dir, "launcher_tweaks.conf")
            if (file.exists()) {
                val props = java.util.Properties()
                java.io.FileInputStream(file).use { props.load(it) }
                enable180 = props.getProperty("enable_180_rotation", "true")?.toBoolean() ?: true
                enableAuto = props.getProperty("enable_auto_rotate", "true")?.toBoolean() ?: true
            }
        } catch (_: Throwable) {}
    }

    private fun saveConfig() {
        TweakConfig.saveFromUi(this, enable180, enableAuto)
    }

    private fun syncConfigToTmp() {
        // 启动时确保配置文件复制到 /data/local/tmp/
        Thread {
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val src = java.io.File(dir, "launcher_tweaks.conf")
                if (!src.exists()) saveConfig()
                if (src.exists()) {
                    val proc = Runtime.getRuntime().exec(arrayOf(
                        "su", "-c",
                        "cp ${src.absolutePath} /data/local/tmp/launcher_tweaks.conf && chmod 644 /data/local/tmp/launcher_tweaks.conf"
                    ))
                    proc.waitFor()
                }
            } catch (_: Throwable) {}
        }.start()
    }

    private fun restartLauncher() {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c",
                "am force-stop com.android.launcher3 && " +
                "monkey -p com.android.launcher3 -c android.intent.category.HOME 1"
            ))
            Toast.makeText(this, "桌面正在重启...", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(this, "重启失败: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
