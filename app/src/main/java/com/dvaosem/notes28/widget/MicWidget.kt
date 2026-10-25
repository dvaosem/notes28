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

/** 1x1 widget: just a small round microphone. One tap starts dictation (DictateActivity), the app itself does not open. */
class MicWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPrefs.removeMic(context, id)
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            for (id in mgr.getAppWidgetIds(ComponentName(context, MicWidget::class.java))) updateWidget(context, mgr, id)
        }

        fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_mic)
            val bg: Int
            val fg: Int
            when (WidgetPrefs.mStyle(context, id)) {
                WidgetPrefs.STYLE_DARK -> { bg = Color.rgb(15, 15, 15); fg = Color.rgb(242, 242, 242) }
                WidgetPrefs.STYLE_LIGHT -> { bg = Color.rgb(250, 250, 250); fg = Color.rgb(26, 26, 26) }
                WidgetPrefs.STYLE_CLEAR -> { bg = Color.rgb(0, 0, 0); fg = Color.WHITE }
                else -> { bg = Color.rgb(58, 57, 54); fg = Color.rgb(243, 242, 236) }
            }
            WidgetBg.apply(context, views, R.id.mic_bg, bg, WidgetPrefs.mOpacity(context, id), 46f, 46f, 23f)
            views.setInt(R.id.mic_icon, "setColorFilter", fg)

            val i = Intent(context, DictateActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(
                context, 7000 + id, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.mic_root, pi)
            mgr.updateAppWidget(id, views)
        }
    }
}
