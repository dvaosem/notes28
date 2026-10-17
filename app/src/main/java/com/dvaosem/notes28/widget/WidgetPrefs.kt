package com.dvaosem.notes28.widget

import android.content.Context
import org.json.JSONArray

/** Notes snapshot pushed by the web app (AndroidBridge.saveNotes) + per-widget settings. */
object WidgetPrefs {
    const val STYLE_DARK = "dark"
    const val STYLE_LIGHT = "light"
    const val STYLE_CLEAR = "clear"

    data class NoteItem(val id: String, val title: String, val content: String)

    private fun sp(c: Context) = c.getSharedPreferences("n28_widget", Context.MODE_PRIVATE)

    /** Returns true when the data changed. */
    fun saveNotesJson(c: Context, json: String): Boolean {
        if (sp(c).getString("notes", null) == json) return false
        sp(c).edit().putString("notes", json).apply()
        return true
    }

    fun notes(c: Context): List<NoteItem> = try {
        val arr = JSONArray(sp(c).getString("notes", "[]"))
        val out = ArrayList<NoteItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id")
            if (id.isNotEmpty()) out.add(NoteItem(id, o.optString("title"), o.optString("content")))
        }
        out
    } catch (e: Exception) {
        emptyList()
    }

    fun find(c: Context, id: String): NoteItem? = notes(c).firstOrNull { it.id == id }

    fun noteId(c: Context, widgetId: Int): String = sp(c).getString("w${widgetId}_note", "") ?: ""
    fun style(c: Context, widgetId: Int): String = sp(c).getString("w${widgetId}_style", STYLE_DARK) ?: STYLE_DARK
    fun opacity(c: Context, widgetId: Int): Int = sp(c).getInt("w${widgetId}_opacity", 90)

    fun saveWidget(c: Context, widgetId: Int, noteId: String, style: String, opacity: Int) {
        sp(c).edit()
            .putString("w${widgetId}_note", noteId)
            .putString("w${widgetId}_style", style)
            .putInt("w${widgetId}_opacity", opacity)
            .apply()
    }

    fun remove(c: Context, widgetId: Int) {
        sp(c).edit().remove("w${widgetId}_note").remove("w${widgetId}_style").remove("w${widgetId}_opacity").apply()
    }
}
