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

    // ---- Quick-note inbox: text typed in the widget dialog, delivered to Firestore by the web app on next open ----
    const val QUICK_ID = "widgetnote"

    @Synchronized
    fun inboxList(c: Context): List<String> = try {
        val arr = JSONArray(sp(c).getString("inbox", "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }

    @Synchronized
    fun addInbox(c: Context, text: String) {
        val l = inboxList(c) + text
        sp(c).edit().putString("inbox", JSONArray(l).toString()).apply()
    }

    @Synchronized
    fun clearInbox(c: Context, n: Int) {
        val l = inboxList(c).drop(n)
        sp(c).edit().putString("inbox", JSONArray(l).toString()).apply()
    }

    /** Quick note as the widget shows it: synced text + lines not delivered yet. */
    fun quickText(c: Context): String {
        val base = find(c, QUICK_ID)?.content.orEmpty().trim()
        return (listOf(base) + inboxList(c)).filter { it.isNotBlank() }.joinToString("\n")
    }

    fun noteId(c: Context, widgetId: Int): String = sp(c).getString("w${widgetId}_note", "") ?: ""
    fun style(c: Context, widgetId: Int): String = sp(c).getString("w${widgetId}_style", STYLE_DARK) ?: STYLE_DARK
    fun opacity(c: Context, widgetId: Int): Int = sp(c).getInt("w${widgetId}_opacity", 90)

    // quick-note widget appearance (separate keys, default opacity higher for dark/light)
    fun qStyle(c: Context, id: Int): String = sp(c).getString("q${id}_style", STYLE_DARK) ?: STYLE_DARK
    fun qOpacity(c: Context, id: Int): Int = sp(c).getInt("q${id}_opacity", 95)
    fun saveQuick(c: Context, id: Int, style: String, opacity: Int) {
        sp(c).edit().putString("q${id}_style", style).putInt("q${id}_opacity", opacity).apply()
    }

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
