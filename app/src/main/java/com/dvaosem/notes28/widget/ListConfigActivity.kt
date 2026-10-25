package com.dvaosem.notes28.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

/** One screen for the notes-list widget: what to show, the look and the background transparency. */
class ListConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        setContentView(R.layout.activity_list_config)
        val modeGroup = findViewById<RadioGroup>(R.id.lc_mode)
        val styleGroup = findViewById<RadioGroup>(R.id.lc_style)
        val opacity = findViewById<SeekBar>(R.id.lc_opacity)

        modeGroup.check(when (WidgetPrefs.lMode(this, widgetId)) {
            WidgetPrefs.MODE_RECENT -> R.id.lc_mode_recent
            WidgetPrefs.MODE_PINNED -> R.id.lc_mode_pinned
            WidgetPrefs.MODE_MEMORY -> R.id.lc_mode_memory
            else -> R.id.lc_mode_all
        })
        styleGroup.check(when (WidgetPrefs.lStyle(this, widgetId)) {
            WidgetPrefs.STYLE_DARK -> R.id.lc_style_dark
            WidgetPrefs.STYLE_LIGHT -> R.id.lc_style_light
            WidgetPrefs.STYLE_CLEAR -> R.id.lc_style_clear
            else -> R.id.lc_style_gray
        })
        opacity.progress = WidgetPrefs.lOpacity(this, widgetId)

        findViewById<Button>(R.id.lc_save).setOnClickListener {
            val mode = when (modeGroup.checkedRadioButtonId) {
                R.id.lc_mode_recent -> WidgetPrefs.MODE_RECENT
                R.id.lc_mode_pinned -> WidgetPrefs.MODE_PINNED
                R.id.lc_mode_memory -> WidgetPrefs.MODE_MEMORY
                else -> WidgetPrefs.MODE_ALL
            }
            val style = when (styleGroup.checkedRadioButtonId) {
                R.id.lc_style_dark -> WidgetPrefs.STYLE_DARK
                R.id.lc_style_light -> WidgetPrefs.STYLE_LIGHT
                R.id.lc_style_clear -> WidgetPrefs.STYLE_CLEAR
                else -> WidgetPrefs.STYLE_GRAY
            }
            WidgetPrefs.saveList(this, widgetId, mode, style, opacity.progress)
            NotesListWidget.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
