package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.dvaosem.notes28.R

class CheckListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0), intent.getBooleanExtra("quick", false))

    private class Factory(val ctx: Context, val widgetId: Int, val quick: Boolean) : RemoteViewsFactory {
        var noteId = ""
        var rows: List<WidgetPrefs.CheckItem> = emptyList()

        private fun load() {
            if (quick) {
                noteId = WidgetPrefs.QUICK_ID
                rows = WidgetPrefs.quickRows(ctx)
            } else {
                noteId = WidgetPrefs.noteId(ctx, widgetId)
                rows = if (noteId.isEmpty()) emptyList() else WidgetPrefs.find(ctx, noteId)?.items.orEmpty()
            }
        }

        override fun onCreate() = load()
        override fun onDataSetChanged() = load()
        override fun onDestroy() {}
        override fun getCount() = rows.size
        override fun getViewAt(pos: Int): RemoteViews {
            val it = rows.getOrNull(pos) ?: return RemoteViews(ctx.packageName, R.layout.widget_check_row)
            val style = if (quick) WidgetPrefs.qStyle(ctx, widgetId) else WidgetPrefs.style(ctx, widgetId)
            val fg: Int
            val sub: Int
            when (style) {
                WidgetPrefs.STYLE_GRAY -> { fg = Color.rgb(243, 242, 236); sub = Color.rgb(150, 148, 140) }
                WidgetPrefs.STYLE_LIGHT -> { fg = Color.rgb(26, 26, 26); sub = Color.rgb(130, 130, 130) }
                else -> { fg = Color.rgb(242, 242, 242); sub = Color.rgb(150, 150, 150) }
            }
            val v = RemoteViews(ctx.packageName, R.layout.widget_check_row)
            v.setTextViewText(R.id.cr_text, it.text)
            if (it.check) {
                v.setViewVisibility(R.id.cr_box, android.view.View.VISIBLE)
                v.setImageViewResource(R.id.cr_box, if (it.checked) R.drawable.ic_cb_on else R.drawable.ic_cb_off)
                v.setInt(R.id.cr_box, "setColorFilter", if (it.checked) sub else fg)
                v.setTextColor(R.id.cr_text, if (it.checked) sub else fg)
                val flags = if (it.checked) Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG else Paint.ANTI_ALIAS_FLAG
                v.setInt(R.id.cr_text, "setPaintFlags", flags)
                v.setOnClickFillInIntent(R.id.cr_root, Intent().putExtra(NoteWidget.EXTRA_POS, pos).putExtra(NoteWidget.EXTRA_NOTE, noteId))
            } else {
                v.setViewVisibility(R.id.cr_box, android.view.View.GONE)
                v.setTextColor(R.id.cr_text, fg)
                v.setInt(R.id.cr_text, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            }
            return v
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(p: Int) = p.toLong()
        override fun hasStableIds() = false
    }
}
