package com.gtlx.launchertweaks.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import com.gtlx.launchertweaks.config.TweakConfig

/**
 * 设置界面 —— 桌面调教
 *
 * 风格：Soft UI + Glassmorphism（emerald → teal）
 */
class MainActivity : Activity() {

    private var enable180 = true
    private var enableAuto = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadConfig()

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#f0fdf4"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }

        // 顶部卡片
        container.addView(buildHeaderCard())

        // 旋转方向
        container.addView(buildSectionTitle("📱 旋转方向"))
        container.addView(buildSwitchCard(
            "允许 180° 反向竖屏",
            "手机倒过来也能正常使用桌面（默认 Launcher 锁了方向）",
            enable180
        ) {
            enable180 = it
            saveConfig()
        })

        // 操作
        container.addView(buildSectionTitle("⚡ 操作"))
        container.addView(buildButtonRow())

        // 说明
        container.addView(buildInfoCard(
            "💡 使用说明",
            "• 修改后点「重启桌面」生效\n" +
            "• 180° 旋转需要系统开启「自动旋转」\n" +
            "• 支持 LineageOS Trebuchet / AOSP Launcher3 / Pixel Launcher\n" +
            "• 完全不需要 root 权限"
        ))

        root.addView(container)
        setContentView(root)
    }

    private fun buildHeaderCard(): android.view.View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
            background = glassCardBg()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        }
        card.addView(TextView(this).apply {
            text = "🍀 桌面调教"
            textSize = 24f
            setTextColor(Color.parseColor("#065f46"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = "v0.1.0 · 让桌面更自由"
            textSize = 13f
            setTextColor(Color.parseColor("#059669"))
            setPadding(0, dp(6), 0, 0)
        })
        return card
    }

    private fun buildSectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.parseColor("#064e3b"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(4), dp(20), dp(4), dp(10))
        }
    }

    private fun buildSwitchCard(
        title: String, desc: String, checked: Boolean,
        onChecked: (Boolean) -> Unit
    ): android.view.View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = glassCardBg()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        textCol.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(Color.parseColor("#1f2937"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        textCol.addView(TextView(this).apply {
            text = desc
            textSize = 12f
            setTextColor(Color.parseColor("#6b7280"))
            setPadding(0, dp(4), 0, 0)
            lineHeight = dp(18)
        })
        val sw = Switch(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onChecked(isChecked) }
        }
        row.addView(textCol)
        row.addView(sw)
        card.addView(row)
        return card
    }

    private fun buildButtonRow(): android.view.View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val btn = Button(this).apply {
            text = "重启桌面"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = gradientButtonBg()
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setOnClickListener { restartLauncher() }
        }
        row.addView(btn)
        return row
    }

    private fun buildInfoCard(title: String, content: String): android.view.View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = infoCardBg()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(24) }
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#047857"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })
        card.addView(TextView(this).apply {
            text = content
            textSize = 12f
            setTextColor(Color.parseColor("#065f46"))
            lineHeight = dp(18)
        })
        return card
    }

    // ===== 背景样式 =====
    private fun glassCardBg(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(Color.parseColor("#ffffff"))
            setStroke(dp(1), Color.parseColor("#d1fae5"))
        }
    }

    private fun infoCardBg(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(Color.parseColor("#ecfdf5"))
        }
    }

    private fun gradientButtonBg(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#10b981"), Color.parseColor("#0d9488"))
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    private fun loadConfig() {
        // 从本地文件读
        try {
            val file = java.io.File(filesDir, "launcher_tweaks.conf")
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

    private fun restartLauncher() {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c",
                "am force-stop com.android.launcher3"))
            Toast.makeText(this, "桌面正在重启...", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(this, "需要 root 才能重启桌面", Toast.LENGTH_SHORT).show()
        }
    }
}
