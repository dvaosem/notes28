package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

/** Appearance of the quick-note widget: theme + background opacity. */
class QuickConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        setContentView(R.layout.activity_quick_config)

        val styleGroup = findViewById<RadioGroup>(R.id.qcfg_style)
        when (WidgetPrefs.qStyle(this, id)) {
            WidgetPrefs.STYLE_LIGHT -> styleGroup.check(R.id.qcfg_style_light)
            WidgetPrefs.STYLE_CLEAR -> styleGroup.check(R.id.qcfg_style_clear)
            else -> styleGroup.check(R.id.qcfg_style_dark)
        }
        val opacity = findViewById<SeekBar>(R.id.qcfg_opacity)
        opacity.progress = WidgetPrefs.qOpacity(this, id)

        findViewById<Button>(R.id.qcfg_save).setOnClickListener {
            val style = when (styleGroup.checkedRadioButtonId) {
                R.id.qcfg_style_light -> WidgetPrefs.STYLE_LIGHT
                R.id.qcfg_style_clear -> WidgetPrefs.STYLE_CLEAR
                else -> WidgetPrefs.STYLE_DARK
            }
            WidgetPrefs.saveQuick(this, id, style, opacity.progress)
            QuickWidget.updateWidget(this, AppWidgetManager.getInstance(this), id)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            finish()
        }
    }
}
