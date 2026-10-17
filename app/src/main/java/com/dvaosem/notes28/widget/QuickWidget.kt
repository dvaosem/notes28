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

/** Widget for the quick note: shows its last lines, tap "Napíš poznámku" and type without opening the app. */
class QuickWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // appearance keys are tiny; leave them
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, QuickWidget::class.java))
            for (id in ids) updateWidget(context, mgr, id)
        }

        private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick)
            val style = WidgetPrefs.qStyle(context, id)
            val bg: Int; val pill: Int; val title: Int; val fg: Int; val sub: Int; val muted: Int; var pillAlpha = 255
            when (style) {
                WidgetPrefs.STYLE_GRAY -> { bg = Color.rgb(38, 38, 36); pill = Color.rgb(58, 57, 54); title = Color.rgb(176, 132, 99); fg = Color.rgb(243, 242, 236); sub = Color.rgb(200, 198, 190); muted = Color.rgb(168, 166, 157) }
                WidgetPrefs.STYLE_LIGHT -> { bg = Color.rgb(250, 250, 250); pill = Color.rgb(232, 232, 232); title = Color.rgb(84, 112, 0); fg = Color.rgb(26, 26, 26); sub = Color.rgb(60, 60, 60); muted = Color.rgb(120, 120, 120) }
                WidgetPrefs.STYLE_CLEAR -> { bg = Color.BLACK; pill = Color.BLACK; pillAlpha = 110; title = Color.rgb(200, 241, 53); fg = Color.WHITE; sub = Color.rgb(235, 235, 235); muted = Color.rgb(200, 200, 200) }
                else -> { bg = Color.rgb(15, 15, 15); pill = Color.rgb(38, 38, 38); title = Color.rgb(200, 241, 53); fg = Color.rgb(242, 242, 242); sub = Color.rgb(216, 216, 216); muted = Color.rgb(160, 160, 160) }
            }
            views.setInt(R.id.qw_bg, "setColorFilter", bg)
            views.setInt(R.id.qw_bg, "setImageAlpha", (WidgetPrefs.qOpacity(context, id).coerceIn(0, 100) * 255) / 100)
            views.setInt(R.id.qw_pill, "setColorFilter", pill)
            views.setInt(R.id.qw_pill, "setImageAlpha", pillAlpha)
            views.setTextColor(R.id.qw_title, title)
            views.setTextColor(R.id.qw_write_text, fg)
            views.setTextColor(R.id.qw_text, sub)
            views.setTextColor(R.id.qw_cfg, muted)

            val cfg = Intent(context, QuickConfigActivity::class.java).apply {
                action = "com.dvaosem.notes28.QW_CFG_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            views.setOnClickPendingIntent(R.id.qw_cfg, PendingIntent.getActivity(context, id * 3 + 2000, cfg, flags()))

            val lines = WidgetPrefs.quickText(context).lines().filter { it.isNotBlank() }.takeLast(6)
            views.setTextViewText(R.id.qw_text, if (lines.isEmpty()) context.getString(R.string.qw_empty) else lines.joinToString("\n"))

            val write = Intent(context, QuickNoteActivity::class.java).apply {
                action = "com.dvaosem.notes28.QW_WRITE_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            views.setOnClickPendingIntent(R.id.qw_write, PendingIntent.getActivity(context, id * 3, write, flags()))

            val open = Intent(context, MainActivity::class.java).apply {
                action = "com.dvaosem.notes28.QW_OPEN_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_NOTE_ID, WidgetPrefs.QUICK_ID)
            }
            views.setOnClickPendingIntent(R.id.qw_root, PendingIntent.getActivity(context, id * 3 + 1, open, flags()))

            mgr.updateAppWidget(id, views)
        }
    }
}
