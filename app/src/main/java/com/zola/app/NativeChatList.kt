package com.zola.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NativeChatList(
    context: Context,
    private val onItemClick: (Conversation) -> Unit
) : FrameLayout(context) {

    private val items = mutableListOf<Conversation>()
    private val adapter: ChatAdapter
    private val listView: ListView
    private val cacheFile = File(context.filesDir, "convlist.json")
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        setBackgroundColor(Color.WHITE)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        val header = createHeaderView(context)
        layout.addView(header)

        listView = ListView(context).apply {
            divider = null
            dividerHeight = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        adapter = ChatAdapter()
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position in items.indices) {
                onItemClick(items[position])
            }
        }
        layout.addView(listView)

        addView(layout)
        loadCachedData()
    }

    fun hasData(): Boolean = items.isNotEmpty()

    private fun createHeaderView(context: Context): LinearLayout {
        val dp = context.resources.displayMetrics.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0068FF"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val titleView = TextView(context).apply {
                text = "Zola"
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(titleView)

            val searchView = TextView(context).apply {
                text = "🔍"
                textSize = 18f
                setTextColor(Color.WHITE)
                setPadding((8 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            }
            addView(searchView)
        }
    }

    fun setData(jsonString: String) {
        try {
            val array = JSONArray(jsonString)
            val newItems = mutableListOf<Conversation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                newItems.add(
                    Conversation(
                        id = obj.optString("id"),
                        name = obj.optString("name", "User"),
                        isGroup = obj.optBoolean("isGroup", false),
                        avatar = obj.optString("avatar", ""),
                        lastMessage = obj.optString("lastMessage", ""),
                        time = obj.optLong("time", 0L),
                        unread = obj.optInt("unread", 0)
                    )
                )
            }
            items.clear()
            items.addAll(newItems)
            adapter.notifyDataSetChanged()
            saveToCache(jsonString)
        } catch (_: Exception) {}
    }

    private fun saveToCache(raw: String) {
        try {
            cacheFile.writeText(raw)
        } catch (_: Exception) {}
    }

    private fun loadCachedData() {
        if (cacheFile.exists()) {
            try {
                setData(cacheFile.readText())
            } catch (_: Exception) {}
        }
    }

    private inner class ChatAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Conversation = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView as? LinearLayout ?: createRowView()
            val item = getItem(position)

            val avatarView = view.findViewById<TextView>(1001)
            val nameView = view.findViewById<TextView>(1002)
            val messageView = view.findViewById<TextView>(1003)
            val timeView = view.findViewById<TextView>(1004)
            val badgeView = view.findViewById<TextView>(1005)

            nameView.text = item.name
            messageView.text = if (item.lastMessage.isBlank()) "Không có tin nhắn" else item.lastMessage
            timeView.text = if (item.time > 0) timeFormat.format(Date(item.time)) else ""

            val initial = item.name.firstOrNull()?.uppercase() ?: "Z"
            avatarView.text = initial

            if (item.unread > 0) {
                badgeView.visibility = View.VISIBLE
                badgeView.text = if (item.unread > 99) "99+" else item.unread.toString()
            } else {
                badgeView.visibility = View.GONE
            }

            return view
        }

        private fun createRowView(): LinearLayout {
            val dp = context.resources.displayMetrics.density
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
                layoutParams = AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT,
                    AbsListView.LayoutParams.WRAP_CONTENT
                )
            }

            val avatar = TextView(context).apply {
                id = 1001
                val size = (48 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    rightMargin = (12 * dp).toInt()
                }
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#0068FF"))
                }
            }
            row.addView(avatar)

            val centerCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val name = TextView(context).apply {
                id = 1002
                setTextColor(Color.parseColor("#081C36"))
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            centerCol.addView(name)

            val message = TextView(context).apply {
                id = 1003
                setTextColor(Color.parseColor("#768494"))
                textSize = 14f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            centerCol.addView(message)
            row.addView(centerCol)

            val rightCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val time = TextView(context).apply {
                id = 1004
                setTextColor(Color.parseColor("#768494"))
                textSize = 12f
            }
            rightCol.addView(time)

            val badge = TextView(context).apply {
                id = 1005
                setTextColor(Color.WHITE)
                textSize = 11f
                gravity = Gravity.CENTER
                val bSize = (18 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(bSize, bSize).apply {
                    topMargin = (4 * dp).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#E11D48"))
                }
                visibility = View.GONE
            }
            rightCol.addView(badge)
            row.addView(rightCol)

            return row
        }
    }
}
