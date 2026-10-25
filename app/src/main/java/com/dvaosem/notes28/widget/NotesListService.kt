package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dvaosem.notes28.R
import com.dvaosem.notes28.ui.MainActivity

class NotesListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))

    private class Factory(val ctx: Context, val widgetId: Int) : RemoteViewsFactory {
        var rows: List<WidgetPrefs.NoteItem> = emptyList()
        var mode = WidgetPrefs.MODE_ALL

        private val boxIds = intArrayOf(R.id.lr_b0, R.id.lr_b1, R.id.lr_b2, R.id.lr_b3, R.id.lr_b4, R.id.lr_b5, R.id.lr_b6, R.id.lr_b7, R.id.lr_b8, R.id.lr_b9)
        private val txtIds = intArrayOf(R.id.lr_t0, R.id.lr_t1, R.id.lr_t2, R.id.lr_t3, R.id.lr_t4, R.id.lr_t5, R.id.lr_t6, R.id.lr_t7, R.id.lr_t8, R.id.lr_t9)
        private val rowIds = intArrayOf(R.id.lr_c0, R.id.lr_c1, R.id.lr_c2, R.id.lr_c3, R.id.lr_c4, R.id.lr_c5, R.id.lr_c6, R.id.lr_c7, R.id.lr_c8, R.id.lr_c9)

        private fun load() {
            mode = WidgetPrefs.lMode(ctx, widgetId)
            rows = WidgetPrefs.listRows(ctx, mode)
        }

        private fun ageText(ms: Long): String {
            val d = ((System.currentTimeMillis() - ms) / 86400000L).toInt()
            return when {
                d < 1 -> "Dnes"
                d < 60 -> "Pred $d dňami"
                d < 365 -> "Pred ${(d / 30.4).toInt()} mesiacmi"
                d < 730 -> "Pred rokom"
                else -> "Pred ${d / 365} rokmi"
            }
        }

        override fun onCreate() = load()
        override fun onDataSetChanged() = load()
        override fun onDestroy() {}
        override fun getCount() = rows.size

        override fun getViewAt(pos: Int): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_list_row)
            val n = rows.getOrNull(pos) ?: return v
            val p = NotesListWidget.palette(WidgetPrefs.lStyle(ctx, widgetId))

            val plain = n.content.trim()
            val firstLine = plain.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            val hasChecks = n.items.any { it.check }
            val title = when {
                n.title.isNotBlank() -> n.title.trim()
                !hasChecks && firstLine.isNotEmpty() -> firstLine.take(60)
                else -> ""
            }
            v.setTextViewText(R.id.lr_title, title)
            v.setTextColor(R.id.lr_title, p.fg)
            v.setViewVisibility(R.id.lr_title, if (title.isEmpty()) View.GONE else View.VISIBLE)

            val mem = mode == WidgetPrefs.MODE_MEMORY
            val bodyLines = plain.lines().filter { it.isNotBlank() }
            val body = (if (n.title.isBlank() && title.isNotEmpty()) bodyLines.drop(1) else bodyLines).joinToString(" ")
            val showSnip = !hasChecks && body.isNotBlank()
            v.setTextViewText(R.id.lr_snip, body.take(200))
            v.setTextColor(R.id.lr_snip, p.sub)
            v.setViewVisibility(R.id.lr_snip, if (showSnip && !mem) View.VISIBLE else View.GONE)
            v.setTextViewText(R.id.lr_snip_long, body.take(320))
            v.setTextColor(R.id.lr_snip_long, p.sub)
            v.setViewVisibility(R.id.lr_snip_long, if (showSnip && mem) View.VISIBLE else View.GONE)

            var kick = ""
            if (mem && n.ts > 0) kick = ageText(n.ts).uppercase()
            else if (n.pin) kick = "PRIPNUTÉ"
            v.setTextViewText(R.id.lr_kick, kick)
            v.setTextColor(R.id.lr_kick, p.muted)
            v.setViewVisibility(R.id.lr_kick, if (kick.isEmpty()) View.GONE else View.VISIBLE)

            // checklist rows (tickable): up to 10 lines, the rest as "+N"
            var shown = 0
            var hidden = 0
            if (hasChecks) {
                n.items.forEachIndexed { idx, it ->
                    if (it.text.isBlank()) return@forEachIndexed
                    if (shown >= rowIds.size) { hidden++; return@forEachIndexed }
                    v.setViewVisibility(rowIds[shown], View.VISIBLE)
                    v.setTextViewText(txtIds[shown], it.text)
                    if (it.check) {
                        v.setViewVisibility(boxIds[shown], View.VISIBLE)
                        v.setImageViewResource(boxIds[shown], if (it.checked) R.drawable.ic_cb_on else R.drawable.ic_cb_off)
                        v.setInt(boxIds[shown], "setColorFilter", if (it.checked) p.muted else p.fg)
                        v.setTextColor(txtIds[shown], if (it.checked) p.muted else p.fg)
                        v.setInt(txtIds[shown], "setPaintFlags", if (it.checked) Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG else Paint.ANTI_ALIAS_FLAG)
                        v.setOnClickFillInIntent(rowIds[shown], Intent().putExtra(NoteWidget.EXTRA_NOTE, n.id).putExtra(NoteWidget.EXTRA_POS, idx))
                    } else {
                        v.setViewVisibility(boxIds[shown], View.INVISIBLE)
                        v.setTextColor(txtIds[shown], p.sub)
                        v.setInt(txtIds[shown], "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                    }
                    shown++
                }
            }
            for (i in shown until rowIds.size) v.setViewVisibility(rowIds[i], View.GONE)
            v.setTextViewText(R.id.lr_more, "+ $hidden ďalších")
            v.setTextColor(R.id.lr_more, p.muted)
            v.setViewVisibility(R.id.lr_more, if (hidden > 0) View.VISIBLE else View.GONE)

            // card background follows the widget transparency (baked ARGB bitmap, height estimated from content)
            val titleLines = if (title.isEmpty()) 0 else if (title.length > 28) 2 else 1
            val snipLines = if (!showSnip) 0 else if (mem) minOf(6, body.length / 45 + 1) else minOf(3, body.length / 45 + 1)
            val hDp = 20f + (if (kick.isEmpty()) 0f else 14f) + titleLines * 21f + snipLines * 15f + shown * 30f + (if (hidden > 0) 18f else 0f)
            WidgetBg.apply(ctx, v, R.id.lr_card, p.card, WidgetPrefs.lOpacity(ctx, widgetId), 300f, hDp.coerceAtLeast(40f), 14f, 0.5f)

            v.setOnClickFillInIntent(R.id.lr_body, Intent().putExtra(MainActivity.EXTRA_NOTE_ID, n.id))
            v.setOnClickFillInIntent(R.id.lr_card, Intent().putExtra(MainActivity.EXTRA_NOTE_ID, n.id))
            return v
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(p: Int) = p.toLong()
        override fun hasStableIds() = false
    }
}
