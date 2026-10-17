package com.dvaosem.notes28.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.dvaosem.notes28.R
import com.dvaosem.notes28.ui.MainActivity

/** Widget for the quick note: shows its last lines, tap "Napíš poznámku" and type without opening the app. */
class QuickWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
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
            val lines = WidgetPrefs.quickText(context).lines().filter { it.isNotBlank() }.takeLast(6)
            views.setTextViewText(R.id.qw_text, if (lines.isEmpty()) context.getString(R.string.qw_empty) else lines.joinToString("\n"))

            val write = Intent(context, QuickNoteActivity::class.java).apply {
                action = "com.dvaosem.notes28.QW_WRITE_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            views.setOnClickPendingIntent(R.id.qw_write, PendingIntent.getActivity(context, id * 2, write, flags()))

            val open = Intent(context, MainActivity::class.java).apply {
                action = "com.dvaosem.notes28.QW_OPEN_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_QUICK, true)
            }
            views.setOnClickPendingIntent(R.id.qw_root, PendingIntent.getActivity(context, id * 2 + 1, open, flags()))

            mgr.updateAppWidget(id, views)
        }
    }
}
