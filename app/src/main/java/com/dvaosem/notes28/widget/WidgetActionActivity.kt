package com.dvaosem.notes28.widget

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.ui.MainActivity

/**
 * Invisible trampoline for taps inside the notes-list widget:
 * a checkbox tap flips the item (the widget refreshes), a tap on the card opens that note.
 */
class WidgetActionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pos = intent.getIntExtra(NoteWidget.EXTRA_POS, -1)
        val tick = intent.getStringExtra(NoteWidget.EXTRA_NOTE).orEmpty()
        val open = intent.getStringExtra(MainActivity.EXTRA_NOTE_ID).orEmpty()
        if (pos >= 0 && tick.isNotEmpty()) {
            if (WidgetPrefs.toggleItem(this, tick, pos)) NoteWidget.refreshAll(this)
        } else if (open.isNotEmpty()) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_NOTE_ID, open)
            })
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
