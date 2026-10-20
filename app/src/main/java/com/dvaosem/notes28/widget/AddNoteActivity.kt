package com.dvaosem.notes28.widget

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R
import com.dvaosem.notes28.ui.MainActivity

/** "Add note" menu opened by the + button of the note widget (text / list / voice / photo). */
class AddNoteActivity : AppCompatActivity() {

    private fun openApp(extra: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(extra, true)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        findViewById<View>(R.id.a_root).setOnClickListener { finish() }
        findViewById<View>(R.id.a_text).setOnClickListener {
            startActivity(Intent(this, QuickNoteActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); finish()
        }
        findViewById<View>(R.id.a_list).setOnClickListener { openApp(MainActivity.EXTRA_NEWLIST); finish() }
        findViewById<View>(R.id.a_voice).setOnClickListener {
            startActivity(Intent(this, DictateActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); finish()
        }
        findViewById<View>(R.id.a_photo).setOnClickListener { openApp(MainActivity.EXTRA_SCAN); finish() }
    }
}
