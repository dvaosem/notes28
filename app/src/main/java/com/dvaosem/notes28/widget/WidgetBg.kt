package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews

/**
 * Widget background drawn as a real ARGB bitmap (alpha baked into the colour).
 * Colour-filter + image-alpha on a shape drawable is unreliable through RemoteViews, so transparency did not change.
 */
object WidgetBg {

    /** Rounded rectangle (or circle when radiusDp is large) filled with [rgb] at [opacityPct] %. */
    fun apply(context: Context, views: RemoteViews, viewId: Int, rgb: Int, opacityPct: Int, widthDp: Float, heightDp: Float, radiusDp: Float) {
        val d = context.resources.displayMetrics.density
        var w = (widthDp * d).coerceAtLeast(8f)
        var h = (heightDp * d).coerceAtLeast(8f)
        var r = radiusDp * d
        val maxPx = 900f * 900f
        if (w * h > maxPx) { val k = Math.sqrt((maxPx / (w * h)).toDouble()).toFloat(); w *= k; h *= k; r *= k }
        val bmp = Bitmap.createBitmap(w.toInt().coerceAtLeast(8), h.toInt().coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val a = (opacityPct.coerceIn(0, 100) * 255) / 100
        p.color = (a shl 24) or (rgb and 0xFFFFFF)
        Canvas(bmp).drawRoundRect(RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), r, r, p)
        views.setImageViewBitmap(viewId, bmp)
    }

    /** Current size of a resizable widget in dp (portrait: min width / max height, landscape: max width / min height). */
    fun sizeDp(context: Context, mgr: AppWidgetManager, id: Int): Pair<Float, Float> {
        val o = mgr.getAppWidgetOptions(id)
        val land = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).let { mn -> if (land) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, mn) else mn }
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).let { mn -> if (land) mn else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, mn) }
        return Pair(if (w > 0) w.toFloat() else 250f, if (h > 0) h.toFloat() else 180f)
    }
}
