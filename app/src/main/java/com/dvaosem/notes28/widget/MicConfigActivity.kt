package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

/** Appearance of the mic widget: color theme + background transparency. */
class MicConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        setContentView(R.layout.activity_mic_config)
        val styleGroup = findViewById<RadioGroup>(R.id.mic_style)
        when (WidgetPrefs.mStyle(this, widgetId)) {
            WidgetPrefs.STYLE_DARK -> styleGroup.check(R.id.mic_style_dark)
            WidgetPrefs.STYLE_LIGHT -> styleGroup.check(R.id.mic_style_light)
            WidgetPrefs.STYLE_CLEAR -> styleGroup.check(R.id.mic_style_clear)
            else -> styleGroup.check(R.id.mic_style_gray)
        }
        val opacity = findViewById<SeekBar>(R.id.mic_opacity)
        opacity.progress = WidgetPrefs.mOpacity(this, widgetId)

        findViewById<Button>(R.id.mic_save).setOnClickListener {
            val style = when (styleGroup.checkedRadioButtonId) {
                R.id.mic_style_dark -> WidgetPrefs.STYLE_DARK
                R.id.mic_style_light -> WidgetPrefs.STYLE_LIGHT
                R.id.mic_style_clear -> WidgetPrefs.STYLE_CLEAR
                else -> WidgetPrefs.STYLE_GRAY
            }
            WidgetPrefs.saveMic(this, widgetId, style, opacity.progress)
            MicWidget.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
