package com.dvaosem.notes28.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.dvaosem.notes28.R
import com.dvaosem.notes28.ui.MainActivity

/** Several notes at once: pinned, latest, or one "memory" (an old note) per day. */
class NotesListWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPrefs.removeList(context, id)
    }

    companion object {
        class Palette(val bg: Int, val fg: Int, val sub: Int, val muted: Int, val accent: Int, val card: Int)

        fun palette(style: String): Palette = when (style) {
            WidgetPrefs.STYLE_LIGHT -> Palette(Color.rgb(250, 250, 250), Color.rgb(26, 26, 26), Color.rgb(90, 90, 90), Color.rgb(120, 120, 120), Color.rgb(122, 90, 62), Color.rgb(240, 238, 232))
            WidgetPrefs.STYLE_DARK -> Palette(Color.rgb(15, 15, 15), Color.rgb(242, 242, 242), Color.rgb(190, 190, 190), Color.rgb(160, 160, 160), Color.rgb(176, 132, 99), Color.rgb(32, 32, 31))
            else -> Palette(Color.rgb(38, 38, 36), Color.rgb(243, 242, 236), Color.rgb(200, 198, 190), Color.rgb(168, 166, 157), Color.rgb(176, 132, 99), Color.rgb(52, 51, 48))
        }

        private val lastHas = HashMap<Int, Boolean>()

        /** Data-only refresh (no full redraw, so no flicker / scroll jump); a full update only when empty<->filled changes. */
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, NotesListWidget::class.java))
            for (id in ids) {
                val has = WidgetPrefs.listRows(context, WidgetPrefs.lMode(context, id)).isNotEmpty()
                if (lastHas[id] != has) { lastHas[id] = has; updateWidget(context, mgr, id) }
                else mgr.notifyAppWidgetViewDataChanged(id, R.id.l_list)
            }
        }

        private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun titleFor(context: Context, mode: String): String = when (mode) {
            WidgetPrefs.MODE_ALL -> context.getString(R.string.l_mode_all)
            WidgetPrefs.MODE_PINNED -> context.getString(R.string.l_mode_pinned)
            WidgetPrefs.MODE_MEMORY -> context.getString(R.string.l_mode_memory)
            else -> context.getString(R.string.l_mode_recent)
        }

        fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_list)
            val mode = WidgetPrefs.lMode(context, id)
            val p = palette(WidgetPrefs.lStyle(context, id))
            views.setInt(R.id.l_bg, "setColorFilter", p.bg)
            views.setInt(R.id.l_bg, "setImageAlpha", (WidgetPrefs.lOpacity(context, id).coerceIn(0, 100) * 255) / 100)
            views.setTextViewText(R.id.l_title, titleFor(context, mode))
            views.setTextColor(R.id.l_title, p.accent)
            views.setTextColor(R.id.l_cfg, p.muted)
            views.setTextColor(R.id.l_empty, p.muted)

            val has = WidgetPrefs.listRows(context, mode).isNotEmpty()
            lastHas[id] = has
            views.setViewVisibility(R.id.l_list, if (has) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.l_empty, if (has) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.l_empty, context.getString(if (mode == WidgetPrefs.MODE_PINNED) R.string.l_empty_pinned else R.string.l_empty))

            val svc = Intent(context, NotesListService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.l_list, svc)

            // tap on a row -> open that note (template + per-row fill-in intent)
            val tpl = Intent(context, WidgetActionActivity::class.java).apply {
                action = "com.dvaosem.notes28.LIST_ACT_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            views.setPendingIntentTemplate(R.id.l_list, PendingIntent.getActivity(context, id * 3 + 6000, tpl,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))

            // tap on the header -> open the app
            val open = Intent(context, MainActivity::class.java).apply {
                action = "com.dvaosem.notes28.LIST_APP_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            views.setOnClickPendingIntent(R.id.l_head, PendingIntent.getActivity(context, id * 3 + 6001, open, flags()))

            val cfg = Intent(context, ListConfigActivity::class.java).apply {
                action = "com.dvaosem.notes28.LIST_CFG_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            views.setOnClickPendingIntent(R.id.l_cfg, PendingIntent.getActivity(context, id * 3 + 6002, cfg, flags()))

            val add = Intent(context, AddNoteActivity::class.java).apply {
                action = "com.dvaosem.notes28.LIST_ADD_$id"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            views.setOnClickPendingIntent(R.id.l_add, PendingIntent.getActivity(context, id * 3 + 6003, add, flags()))

            mgr.updateAppWidget(id, views)
            mgr.notifyAppWidgetViewDataChanged(id, R.id.l_list)
        }
    }
}
