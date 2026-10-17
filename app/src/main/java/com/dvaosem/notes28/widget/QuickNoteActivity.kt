package com.dvaosem.notes28.widget

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

/** Small dialog over the home screen: type a line, save. The app itself does not open. */
class QuickNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        val input = findViewById<EditText>(R.id.q_input)
        input.requestFocus()
        findViewById<View>(R.id.q_root).setOnClickListener { finish() }
        findViewById<View>(R.id.q_cancel).setOnClickListener { finish() }
        findViewById<View>(R.id.q_save).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                WidgetPrefs.addInbox(this, text)
                NoteWidget.refreshAll(this)
                Toast.makeText(this, R.string.q_saved, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
