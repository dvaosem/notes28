package com.dvaosem.notes28.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

/** Two quick questions when adding the notes-list widget: what to show, and the look. */
class ListConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        if (savedInstanceState == null) askMode()
    }

    private fun askMode() {
        val modes = arrayOf(WidgetPrefs.MODE_RECENT, WidgetPrefs.MODE_PINNED, WidgetPrefs.MODE_MEMORY)
        val labels = arrayOf(getString(R.string.l_mode_recent), getString(R.string.l_mode_pinned), getString(R.string.l_mode_memory))
        var sel = modes.indexOf(WidgetPrefs.lMode(this, widgetId)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.l_cfg_what)
            .setSingleChoiceItems(labels, sel) { _, which -> sel = which }
            .setPositiveButton(R.string.l_next) { _, _ -> askStyle(modes[sel]) }
            .setNegativeButton(R.string.q_cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun askStyle(mode: String) {
        val styles = arrayOf(WidgetPrefs.STYLE_GRAY, WidgetPrefs.STYLE_DARK, WidgetPrefs.STYLE_LIGHT)
        val labels = arrayOf(getString(R.string.cfg_style_gray), getString(R.string.cfg_style_dark), getString(R.string.cfg_style_light))
        var sel = styles.indexOf(WidgetPrefs.lStyle(this, widgetId)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.cfg_style)
            .setSingleChoiceItems(labels, sel) { _, which -> sel = which }
            .setPositiveButton(R.string.cfg_save) { _, _ ->
                WidgetPrefs.saveList(this, widgetId, mode, styles[sel], WidgetPrefs.lOpacity(this, widgetId))
                NotesListWidget.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            }
            .setNegativeButton(R.string.q_cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
