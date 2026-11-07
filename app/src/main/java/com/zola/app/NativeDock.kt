package com.zola.app

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class NativeDock(
    context: Context,
    private val onTabSelected: (Int) -> Unit
) : LinearLayout(context) {

    private val tabs = listOf(
        "💬\nTin nhắn" to 0,
        "👥\nDanh bạ" to 1,
        "🧭\nKhám phá" to 2,
        "👤\nCá nhân" to 3
    )

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.parseColor("#F4F5F7"))
        gravity = Gravity.CENTER_VERTICAL
        val dp = context.resources.displayMetrics.density
        setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())

        tabs.forEach { (label, index) ->
            val button = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(if (index == 0) Color.parseColor("#0068FF") else Color.parseColor("#768494"))
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    updateSelection(index)
                    onTabSelected(index)
                }
            }
            addView(button)
        }
    }

    private fun updateSelection(selectedIndex: Int) {
        for (i in 0 until childCount) {
            val tv = getChildAt(i) as? TextView ?: continue
            tv.setTextColor(if (i == selectedIndex) Color.parseColor("#0068FF") else Color.parseColor("#768494"))
        }
    }
}
// ponytail: static 4-tab dock; upgrade to dynamic badges synced via bridge when unread counters require real-time indicator.
