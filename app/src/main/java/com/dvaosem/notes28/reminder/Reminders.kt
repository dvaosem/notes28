package com.dvaosem.notes28.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dvaosem.notes28.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject

/** Exact-time reminders for notes with a due time. The web app sends the full list (JSON) on every change. */
object Reminders {
    const val CHANNEL = "n28_due"
    private const val MAX = 150

    private fun prefs(c: Context) = c.getSharedPreferences("n28_reminders", Context.MODE_PRIVATE)

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Pripomienky poznámok", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    private fun pending(c: Context, id: String, title: String, text: String): PendingIntent {
        val i = Intent(c, ReminderReceiver::class.java).setAction("n28.reminder.$id")
            .putExtra("id", id).putExtra("title", title).putExtra("text", text)
        return PendingIntent.getBroadcast(c, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun canExact(c: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Replaces all scheduled reminders by [json] = [{id, title, text, at(ms)}]. */
    @Synchronized
    fun apply(c: Context, json: String) {
        val am = c.getSystemService(AlarmManager::class.java)
        // cancel what was scheduled before
        try {
            val old = JSONArray(prefs(c).getString("ids", "[]"))
            for (k in 0 until old.length()) {
                val o = old.getJSONObject(k)
                am.cancel(pending(c, o.getString("id"), "", ""))
            }
        } catch (e: Exception) { }
        val now = System.currentTimeMillis()
        val kept = JSONArray()
        try {
            val arr = JSONArray(json)
            var n = 0
            for (k in 0 until arr.length()) {
                if (n >= MAX) break
                val o = arr.getJSONObject(k)
                val id = o.optString("id")
                val at = o.optLong("at")
                if (id.isEmpty() || at <= now) continue
                val pi = pending(c, id, o.optString("title"), o.optString("text"))
                try {
                    if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } catch (e: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
                kept.put(JSONObject().put("id", id).put("at", at))
                n++
            }
            prefs(c).edit().putString("list", json).putString("ids", kept.toString()).apply()
        } catch (e: Exception) {
            prefs(c).edit().putString("ids", "[]").apply()
        }
    }

    /** After reboot / app update the alarms are gone: schedule the stored list again. */
    fun reschedule(c: Context) {
        val json = prefs(c).getString("list", null) ?: return
        apply(c, json)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val id = intent.getStringExtra("id").orEmpty()
        if (id.isEmpty()) return
        val title = intent.getStringExtra("title").orEmpty().ifBlank { "N28" }
        val text = intent.getStringExtra("text").orEmpty()
        Reminders.ensureChannel(context)
        val open = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_NOTE_ID, id)
        }
        val tap = PendingIntent.getActivity(context, id.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, Reminders.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ $title")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        context.getSystemService(android.app.NotificationManager::class.java).notify(id.hashCode(), n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.reschedule(context)
    }
}
