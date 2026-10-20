package com.dvaosem.notes28.widget

import android.content.Context
import org.json.JSONArray

/** Notes snapshot pushed by the web app (AndroidBridge.saveNotes) + per-widget settings. */
object WidgetPrefs {
    const val STYLE_GRAY = "gray"
    const val STYLE_DARK = "dark"
    const val STYLE_LIGHT = "light"
    const val STYLE_CLEAR = "clear"

    data class CheckItem(val text: String, val checked: Boolean, val check: Boolean = false, val n: Int = 0)
    data class NoteItem(val id: String, val title: String, val content: String, val items: List<CheckItem> = emptyList(), val pin: Boolean = false, val ts: Long = 0L)

    private fun sp(c: Context) = c.getSharedPreferences("n28_widget", Context.MODE_PRIVATE)

    /** Returns true when the data changed. */
    fun saveNotesJson(c: Context, json: String): Boolean {
        if (sp(c).getString("notes", null) == json) return false
        sp(c).edit().putString("notes", json).apply()
        return true
    }

    fun notes(c: Context): List<NoteItem> = try {
        val arr = JSONArray(sp(c).getString("notes", "[]"))
        val pend = togglesList(c)
        val out = ArrayList<NoteItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optString("id")
            if (id.isEmpty()) continue
            val its = ArrayList<CheckItem>()
            val ia = o.optJSONArray("items")
            if (ia != null) for (k in 0 until ia.length()) {
                val io = ia.getJSONObject(k)
                val t = io.optString("t")
                val isCheck = io.optInt("k") == 1
                val ord = io.optInt("n")
                var ch = io.optBoolean("c")
                // zmeny z widgetu, ktoré appka ešte nezapísala, majú prednosť pred starým stavom z webu
                if (isCheck) for (p in pend) if (p.id == id && p.index == ord && p.text == t) ch = p.checked
                its.add(CheckItem(t, ch, isCheck, ord))
            }
            out.add(NoteItem(id, o.optString("title"), o.optString("content"), its, o.optInt("pin") == 1, o.optLong("ts")))
        }
        out
    } catch (e: Exception) {
        emptyList()
    }

    fun find(c: Context, id: String): NoteItem? = notes(c).firstOrNull { it.id == id }

    // ---- Checklist toggles tapped in the widget, delivered to Firestore by the web app on next open ----
    data class Toggle(val id: String, val index: Int, val text: String, val checked: Boolean)

    @Synchronized
    fun togglesList(c: Context): List<Toggle> = try {
        val arr = JSONArray(sp(c).getString("toggles", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Toggle(o.optString("id"), o.optInt("i"), o.optString("t"), o.optBoolean("c"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    @Synchronized
    fun togglesJson(c: Context): String = sp(c).getString("toggles", "[]") ?: "[]"

    /** Flips list row [pos] of [noteId]; returns false when the item is gone. */
    @Synchronized
    fun toggleItem(c: Context, noteId: String, pos: Int): Boolean {
        val item = find(c, noteId)?.items?.getOrNull(pos)?.takeIf { it.check } ?: return false
        val arr = JSONArray(sp(c).getString("toggles", "[]"))
        arr.put(org.json.JSONObject().put("id", noteId).put("i", item.n).put("t", item.text).put("c", !item.checked))
        sp(c).edit().putString("toggles", arr.toString()).apply()
        return true
    }

    @Synchronized
    fun clearToggles(c: Context, n: Int) {
        val arr = JSONArray(sp(c).getString("toggles", "[]"))
        val out = JSONArray()
        for (i in n until arr.length()) out.put(arr.get(i))
        sp(c).edit().putString("toggles", out.toString()).apply()
    }

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

    /** Rows of the writing widget: checklist rows of the Widget note + lines typed but not yet delivered. */
    fun quickRows(c: Context): List<CheckItem> =
        find(c, QUICK_ID)?.items.orEmpty() + inboxList(c).flatMap { it.lines() }.filter { it.isNotBlank() }.map { CheckItem(it.trim(), false) }

    fun quickHasChecks(c: Context): Boolean = find(c, QUICK_ID)?.items?.any { it.check } == true

    // notes-list widget: mode = pinned | recent | memory
    const val MODE_PINNED = "pinned"
    const val MODE_RECENT = "recent"
    const val MODE_MEMORY = "memory"
    fun lMode(c: Context, id: Int): String = sp(c).getString("l${id}_mode", MODE_RECENT) ?: MODE_RECENT
    fun lStyle(c: Context, id: Int): String = sp(c).getString("l${id}_style", STYLE_GRAY) ?: STYLE_GRAY
    fun lOpacity(c: Context, id: Int): Int = sp(c).getInt("l${id}_opacity", 92)
    fun saveList(c: Context, id: Int, mode: String, style: String, opacity: Int) {
        sp(c).edit().putString("l${id}_mode", mode).putString("l${id}_style", style).putInt("l${id}_opacity", opacity).apply()
    }
    fun removeList(c: Context, id: Int) {
        sp(c).edit().remove("l${id}_mode").remove("l${id}_style").remove("l${id}_opacity").apply()
    }

    private fun special(n: NoteItem) = n.id == "quicknote" || n.id == QUICK_ID

    /** Rows for the notes-list widget by [mode]. */
    fun listRows(c: Context, mode: String): List<NoteItem> {
        val all = notes(c).filter { !special(it) && (it.title.isNotBlank() || it.content.isNotBlank()) }
        return when (mode) {
            MODE_PINNED -> all.filter { it.pin }.take(12)
            MODE_MEMORY -> {
                val old = all.filter { it.ts > 0 && System.currentTimeMillis() - it.ts > 45L * 86400000L && it.content.trim().length >= 40 }
                if (old.isEmpty()) emptyList() else {
                    val day = (System.currentTimeMillis() / 86400000L).toInt()
                    listOf(old[Math.floorMod(day * 31 + 7, old.size)])
                }
            }
            else -> all.sortedByDescending { it.ts }.take(12)
        }
    }

    fun noteId(c: Context, widgetId: Int): String = sp(c).getString("w${widgetId}_note", "") ?: ""
    fun style(c: Context, widgetId: Int): String = sp(c).getString("w${widgetId}_style", STYLE_GRAY) ?: STYLE_GRAY
    fun opacity(c: Context, widgetId: Int): Int = sp(c).getInt("w${widgetId}_opacity", 90)

    // quick-note widget appearance (separate keys, default opacity higher for dark/light)
    fun qStyle(c: Context, id: Int): String = sp(c).getString("q${id}_style", STYLE_GRAY) ?: STYLE_GRAY
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
