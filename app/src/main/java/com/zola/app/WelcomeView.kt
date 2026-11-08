package com.zola.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class WelcomeView(context: Context) : FrameLayout(context) {
    private val iconView: ImageView
    private val titleView: TextView
    private val descView: TextView
    private val hintView: TextView
    private val container: LinearLayout

    init {
        isClickable = true
        isFocusable = true
        val isVi = Locale.getDefault().language == "vi"
        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        iconView = ImageView(context).apply {
            setImageResource(R.drawable.ic_welcome_chat)
            val lp = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                bottomMargin = dp(16)
            }
            layoutParams = lp
        }

        titleView = TextView(context).apply {
            text = if (isVi) "Chào mừng bạn đến với Zola!" else "Welcome to Zola!"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
            layoutParams = lp
        }

        descView = TextView(context).apply {
            text = if (isVi) {
                "Khám phá những tiện ích hỗ trợ trò chuyện và làm việc tối ưu trên màn hình lớn."
            } else {
                "Explore chat and productivity tools optimized for large screens."
            }
            textSize = 14f
            gravity = Gravity.CENTER
            maxWidth = dp(400)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
            layoutParams = lp
        }

        hintView = TextView(context).apply {
            text = if (isVi) "Chọn một hội thoại để bắt đầu trò chuyện" else "Select a conversation to start chatting"
            textSize = 13f
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }

        container.addView(iconView)
        container.addView(titleView)
        container.addView(descView)
        container.addView(hintView)

        val centerParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        addView(container, centerParams)
        applyColors(isNightMode())
    }

    fun applyColors(night: Boolean) {
        setBackgroundColor(if (night) Color.parseColor("#22262B") else Color.WHITE)
        titleView.setTextColor(if (night) Color.parseColor("#FFFFFF") else Color.parseColor("#0068FF"))
        descView.setTextColor(if (night) Color.parseColor("#8C99AB") else Color.parseColor("#77859C"))
        hintView.setTextColor(if (night) Color.parseColor("#596473") else Color.parseColor("#9AA2B2"))
    }

    private fun isNightMode(): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun dp(v: Int): Int = Math.round(v * context.resources.displayMetrics.density)
}
