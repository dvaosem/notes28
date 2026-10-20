package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dvaosem.notes28.R
import com.dvaosem.notes28.ui.MainActivity

class NotesListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))

    private class Factory(val ctx: Context, val widgetId: Int) : RemoteViewsFactory {
        var rows: List<WidgetPrefs.NoteItem> = emptyList()
        var mode = WidgetPrefs.MODE_RECENT

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
            val title = when {
                n.title.isNotBlank() -> n.title.trim()
                firstLine.isNotEmpty() -> firstLine.take(48)
                else -> ctx.getString(R.string.w_untitled)
            }
            // snippet: skip the line that was used as the title
            val body = if (n.title.isBlank()) plain.lines().filter { it.isNotBlank() }.drop(1).joinToString(" ") else plain.lines().filter { it.isNotBlank() }.joinToString(" ")
            v.setTextViewText(R.id.lr_title, title)
            v.setTextColor(R.id.lr_title, p.fg)
            val mem = mode == WidgetPrefs.MODE_MEMORY
            v.setTextViewText(R.id.lr_snip, body.take(90))
            v.setTextColor(R.id.lr_snip, p.sub)
            v.setViewVisibility(R.id.lr_snip, if (body.isBlank() || mem) android.view.View.GONE else android.view.View.VISIBLE)
            v.setTextViewText(R.id.lr_snip_long, body.take(240))
            v.setTextColor(R.id.lr_snip_long, p.sub)
            v.setViewVisibility(R.id.lr_snip_long, if (body.isBlank() || !mem) android.view.View.GONE else android.view.View.VISIBLE)
            val kick = if (mode == WidgetPrefs.MODE_MEMORY && n.ts > 0) ageText(n.ts).uppercase() else ""
            v.setTextViewText(R.id.lr_kick, kick)
            v.setTextColor(R.id.lr_kick, p.muted)
            v.setViewVisibility(R.id.lr_kick, if (kick.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE)
            v.setOnClickFillInIntent(R.id.lr_root, Intent().putExtra(MainActivity.EXTRA_NOTE_ID, n.id))
            return v
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(p: Int) = p.toLong()
        override fun hasStableIds() = false
    }
}
