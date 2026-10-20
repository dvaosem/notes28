package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

/** Quick switch of the note shown by a note widget (no need to open the full settings). */
class PickNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wid = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        setContentView(R.layout.activity_pick)
        findViewById<View>(R.id.p_root).setOnClickListener { finish() }

        val notes = WidgetPrefs.notes(this).filter { it.title.isNotBlank() || it.content.isNotBlank() || it.items.isNotEmpty() }
        val current = WidgetPrefs.noteId(this, wid)
        if (notes.isEmpty()) findViewById<View>(R.id.p_empty).visibility = View.VISIBLE

        fun label(n: WidgetPrefs.NoteItem): String {
            val first = n.content.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            val t = when {
                n.title.isNotBlank() -> n.title.trim()
                n.id == "quicknote" -> "Rýchla poznámka"
                first.isNotEmpty() -> first.take(40)
                else -> getString(R.string.w_untitled)
            }
            val checks = n.items.filter { it.check }
            return if (checks.isEmpty()) t else t + "   " + checks.count { it.checked } + "/" + checks.size
        }

        val list = findViewById<ListView>(R.id.p_list)
        list.adapter = object : BaseAdapter() {
            override fun getCount() = notes.size
            override fun getItem(p: Int) = notes[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, parent: ViewGroup): View {
                val tv = (v as? TextView) ?: TextView(this@PickNoteActivity).apply {
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    val pad = (14 * resources.displayMetrics.density).toInt()
                    setPadding(pad, 0, pad, 0)
                    textSize = 15f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                val n = notes[p]
                tv.text = label(n)
                tv.setTextColor(if (n.id == current) Color.rgb(176, 132, 99) else Color.rgb(243, 242, 236))
                return tv
            }
        }
        list.setOnItemClickListener { _, _, pos, _ ->
            WidgetPrefs.saveWidget(this, wid, notes[pos].id, WidgetPrefs.style(this, wid), WidgetPrefs.opacity(this, wid))
            NoteWidget.updateWidget(this, AppWidgetManager.getInstance(this), wid)
            finish()
        }
    }
}
