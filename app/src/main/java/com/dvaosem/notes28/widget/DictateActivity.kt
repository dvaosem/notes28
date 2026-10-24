package com.dvaosem.notes28.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dvaosem.notes28.R

/**
 * Microphone button of the widget: shows the system speech dialog (no extra permission needed),
 * the recognized text goes to the same inbox as the typed quick note. The app itself does not open.
 */
class DictateActivity : AppCompatActivity() {

    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val heard = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim().orEmpty()
            // spoken closing phrase "ulož poznámku" / "save note" is a command, not part of the note
            val text = heard.replace(Regex("[\\s,.]*(ulož(iť)?\\s+poznámku|save\\s+note)[\\s.!]*$", RegexOption.IGNORE_CASE), "").trim()
            if (text.isNotEmpty()) {
                WidgetPrefs.addTitled(this, "", text)
                NoteWidget.refreshAll(this)
                Toast.makeText(this, R.string.q_saved, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sk-SK")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.dict_prompt))
        }
        try {
            speech.launch(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.dict_missing, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
