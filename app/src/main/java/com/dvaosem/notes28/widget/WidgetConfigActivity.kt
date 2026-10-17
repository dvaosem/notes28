package com.dvaosem.notes28.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedNote = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        setContentView(R.layout.activity_widget_config)

        val group = findViewById<RadioGroup>(R.id.cfg_notes)
        val notes = WidgetPrefs.notes(this)
        selectedNote = WidgetPrefs.noteId(this, widgetId)
        if (notes.isEmpty()) findViewById<TextView>(R.id.cfg_empty).visibility = View.VISIBLE

        for (n in notes) {
            val firstLine = n.content.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            val label = when {
                n.title.isNotBlank() -> n.title.trim()
                n.id == "quicknote" -> "⚡ " + getString(R.string.w_untitled)
                firstLine.isNotEmpty() -> firstLine.take(40)
                else -> getString(R.string.w_untitled)
            }
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = label
                tag = n.id
                setTextColor(0xFFF2F2F2.toInt())
                setPadding(12, 18, 12, 18)
            }
            group.addView(rb)
            if (n.id == selectedNote) rb.isChecked = true
        }
        group.setOnCheckedChangeListener { g, checkedId ->
            selectedNote = (g.findViewById<View>(checkedId)?.tag as? String) ?: ""
        }

        val styleGroup = findViewById<RadioGroup>(R.id.cfg_style)
        when (WidgetPrefs.style(this, widgetId)) {
            WidgetPrefs.STYLE_LIGHT -> styleGroup.check(R.id.cfg_style_light)
            WidgetPrefs.STYLE_CLEAR -> styleGroup.check(R.id.cfg_style_clear)
            else -> styleGroup.check(R.id.cfg_style_dark)
        }
        val opacity = findViewById<SeekBar>(R.id.cfg_opacity)
        opacity.progress = WidgetPrefs.opacity(this, widgetId)

        findViewById<Button>(R.id.cfg_save).setOnClickListener {
            if (selectedNote.isEmpty()) {
                Toast.makeText(this, R.string.cfg_pick, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val style = when (styleGroup.checkedRadioButtonId) {
                R.id.cfg_style_light -> WidgetPrefs.STYLE_LIGHT
                R.id.cfg_style_clear -> WidgetPrefs.STYLE_CLEAR
                else -> WidgetPrefs.STYLE_DARK
            }
            WidgetPrefs.saveWidget(this, widgetId, selectedNote, style, opacity.progress)
            NoteWidget.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
