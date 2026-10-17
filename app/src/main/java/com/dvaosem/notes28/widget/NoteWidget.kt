package com.dvaosem.notes28.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.dvaosem.notes28.R
import com.dvaosem.notes28.ui.MainActivity

class NoteWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPrefs.remove(context, id)
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))
            for (id in ids) updateWidget(context, mgr, id)
        }

        fun hasWidgets(context: Context): Boolean {
            val mgr = AppWidgetManager.getInstance(context)
            return mgr.getAppWidgetIds(ComponentName(context, NoteWidget::class.java)).isNotEmpty()
        }

        private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_note)
            val style = WidgetPrefs.style(context, id)
            val noteId = WidgetPrefs.noteId(context, id)
            val note = if (noteId.isEmpty()) null else WidgetPrefs.find(context, noteId)

            val bg: Int
            val fg: Int
            val sub: Int
            val muted: Int
            when (style) {
                WidgetPrefs.STYLE_LIGHT -> { bg = Color.rgb(250, 250, 250); fg = Color.rgb(26, 26, 26); sub = Color.rgb(60, 60, 60); muted = Color.rgb(120, 120, 120) }
                WidgetPrefs.STYLE_CLEAR -> { bg = Color.rgb(0, 0, 0); fg = Color.WHITE; sub = Color.rgb(235, 235, 235); muted = Color.rgb(200, 200, 200) }
                else -> { bg = Color.rgb(15, 15, 15); fg = Color.rgb(242, 242, 242); sub = Color.rgb(216, 216, 216); muted = Color.rgb(160, 160, 160) }
            }
            val alpha = (WidgetPrefs.opacity(context, id).coerceIn(0, 100) * 255) / 100
            views.setInt(R.id.w_bg, "setColorFilter", bg)
            views.setInt(R.id.w_bg, "setImageAlpha", alpha)

            val title: String
            val text: String
            if (note == null) {
                title = context.getString(R.string.app_name)
                text = context.getString(R.string.w_hint)
            } else {
                val firstLine = note.content.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                title = when {
                    note.title.isNotBlank() -> note.title.trim()
                    note.id == "quicknote" -> "⚡ " + context.getString(R.string.w_untitled)
                    firstLine.isNotEmpty() -> firstLine.take(40)
                    else -> context.getString(R.string.w_untitled)
                }
                text = note.content.trim()
            }
            views.setTextViewText(R.id.w_title, title)
            views.setTextViewText(R.id.w_text, text)
            views.setTextColor(R.id.w_title, fg)
            views.setTextColor(R.id.w_text, sub)
            views.setTextColor(R.id.w_cfg, muted)

            // tap on the widget -> open the note (or the app)
            val open = Intent(context, MainActivity::class.java).apply {
                action = "com.dvaosem.notes28.OPEN_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (note != null) putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
            }
            views.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(context, id * 3, open, flags()))

            // + -> quick note
            val quick = Intent(context, MainActivity::class.java).apply {
                action = "com.dvaosem.notes28.QUICK_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_QUICK, true)
            }
            views.setOnClickPendingIntent(R.id.w_add, PendingIntent.getActivity(context, id * 3 + 1, quick, flags()))

            // gear -> choose another note / style
            val cfg = Intent(context, WidgetConfigActivity::class.java).apply {
                action = "com.dvaosem.notes28.CFG_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            views.setOnClickPendingIntent(R.id.w_cfg, PendingIntent.getActivity(context, id * 3 + 2, cfg, flags()))

            mgr.updateAppWidget(id, views)
        }
    }
}
