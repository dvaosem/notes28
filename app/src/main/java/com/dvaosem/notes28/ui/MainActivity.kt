package com.dvaosem.notes28.ui

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.dvaosem.notes28.R
import com.dvaosem.notes28.widget.NoteWidget
import com.dvaosem.notes28.widget.WidgetPrefs
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Thin WebView shell around https://dvaosem.com/notes28.html.
 * The web app itself is updated through the hosting (service worker); the APK changes only when this shell does.
 */
class MainActivity : AppCompatActivity() {
    private var bootProc = 0L; private var bootCreate = 0L; private var bootStart = 0L; private var bootFinish = 0L

    companion object {
        const val START_URL = "https://dvaosem.com/notes28.html"
        const val EXTRA_NOTE_ID = "n28_note_id"
        const val EXTRA_QUICK = "n28_quick"
        const val EXTRA_SCAN = "n28_scan"
        const val EXTRA_NEWLIST = "n28_newlist"
        const val EXTRA_DRAW = "n28_draw"
        private val OWN_HOSTS = setOf("dvaosem.com", "www.dvaosem.com")
        private val URL_RE = Regex("https?://\\S+")
        private const val OFFLINE_HTML = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
            "<body style='background:#0f0f0f;color:#f2f2f2;font-family:sans-serif;text-align:center;padding-top:30vh'>" +
            "<h2>Bez pripojenia</h2><p><a style='color:#c8f135;font-size:1.1em' href='$START_URL'>Skúsiť znova</a></p></body></html>"
    }

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingShare: String? = null
    private var pageLoaded = false
    @Volatile private var pendingUpdateUrl: String? = null

    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data))
        fileCallback = null
    }

    private fun js(code: String) {
        runOnUiThread { if (::webView.isInitialized) webView.evaluateJavascript(code, null) }
    }

    /** Calls window.fn(argJson) in the page if it exists. */
    private fun call(fn: String, argJson: String) = js("window.$fn&&window.$fn($argJson)")

    override fun onCreate(savedInstanceState: Bundle?) {
        bootCreate = android.os.SystemClock.uptimeMillis()
        bootProc = android.os.Process.getStartUptimeMillis()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        // pozadie v tóne poslednej témy appky — žiadna čierna plocha pred prvým vykreslením
        try {
            val c = android.graphics.Color.parseColor(getSharedPreferences("n28_ui", MODE_PRIVATE).getString("bg", "#262624"))
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(c))
            webView.setBackgroundColor(c)
        } catch (e: Exception) {}
        ViewCompat.setOnApplyWindowInsetsListener(webView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setupWebView()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        extractShare(intent)
        val url = startUrlFor(intent)
        clearLaunchExtras(intent)
        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) return
        webView.loadUrl(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractShare(intent)
        val note = intent.getStringExtra(EXTRA_NOTE_ID)
        if (!note.isNullOrEmpty()) {
            js("window.openNoteById&&window.openNoteById(${JSONObject.quote(note)})")
        } else if (intent.getBooleanExtra(EXTRA_QUICK, false)) {
            js("window.openQuickNote&&window.openQuickNote()")
        } else if (intent.getBooleanExtra(EXTRA_SCAN, false)) {
            js("window.n28Scan&&window.n28Scan()")
        } else if (intent.getBooleanExtra(EXTRA_NEWLIST, false)) {
            js("window.n28NewList&&window.n28NewList()")
        } else if (intent.getBooleanExtra(EXTRA_DRAW, false)) {
            js("window.n28Draw&&window.n28Draw()")
        }
        clearLaunchExtras(intent)
        if (pageLoaded) deliverShare()
    }

    private fun startUrlFor(i: Intent?): String {
        val note = i?.getStringExtra(EXTRA_NOTE_ID)
        return when {
            !note.isNullOrEmpty() -> "$START_URL?note=" + Uri.encode(note)
            i?.getBooleanExtra(EXTRA_QUICK, false) == true -> "$START_URL?quick=1"
            i?.getBooleanExtra(EXTRA_SCAN, false) == true -> "$START_URL?scan=1"
            i?.getBooleanExtra(EXTRA_NEWLIST, false) == true -> "$START_URL?newlist=1"
            i?.getBooleanExtra(EXTRA_DRAW, false) == true -> "$START_URL?draw=1"
            else -> START_URL
        }
    }

    private fun clearLaunchExtras(i: Intent?) {
        i?.removeExtra(EXTRA_NOTE_ID)
        i?.removeExtra(EXTRA_QUICK)
        i?.removeExtra(EXTRA_SCAN)
        i?.removeExtra(EXTRA_NEWLIST)
        i?.removeExtra(EXTRA_DRAW)
    }

    /** A link shared from the browser (Share -> N28): remembered until the page is ready. */
    private fun extractShare(i: Intent?) {
        if (i == null || i.action != Intent.ACTION_SEND || i.type != "text/plain") return
        val text = i.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (text.isEmpty()) return
        pendingShare = (URL_RE.find(text)?.value ?: text).take(2000)
        i.action = null
    }

    private fun deliverShare() {
        val u = pendingShare ?: return
        pendingShare = null
        val q = JSONObject.quote(u)
        js("(function t(n){if(window.receiveSharedUrl){window.receiveSharedUrl($q)}else if(n>0){setTimeout(function(){t(n-1)},500)}})(40)")
    }

    inner class AndroidBridge {

        /** Startup timing (ms): process->onCreate, onCreate->pageStarted, onCreate->pageFinished. */
        @JavascriptInterface
        fun getBoot(): String = "proc " + (bootCreate - bootProc) + " · wv " + (bootStart - bootCreate) + " · load " + (bootFinish - bootCreate)

        /** Native Android share sheet for plain text (navigator.share is not available in a WebView). */
        @JavascriptInterface
        fun shareText(title: String, text: String): Boolean {
            if (text.length > 500_000) return false
            runOnUiThread {
                try {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    startActivity(Intent.createChooser(send, title))
                } catch (e: Exception) {}
            }
            return true
        }

        /** Web app reports its current theme background so the next cold start paints it natively. */
        @JavascriptInterface
        fun setBg(color: String) {
            if (!Regex("#[0-9a-fA-F]{6}").matches(color)) return
            getSharedPreferences("n28_ui", MODE_PRIVATE).edit().putString("bg", color).apply()
        }

        /** Compact notes list for the widget (pushed by the web app after every Firestore update). */
        @JavascriptInterface
        fun saveNotes(json: String) {
            if (json.length > 2_000_000) return
            if (WidgetPrefs.saveNotesJson(this@MainActivity, json)) NoteWidget.refreshAll(this@MainActivity)
        }

        /** Checklist ticks made in the home-screen widget, waiting to be written to the note. */
        @JavascriptInterface
        fun getToggles(): String = WidgetPrefs.togglesJson(this@MainActivity)

        @JavascriptInterface
        fun clearToggles(n: Int) {
            WidgetPrefs.clearToggles(this@MainActivity, n)
            NoteWidget.refreshAll(this@MainActivity)
        }

        /** Native Google sign-in; answers via window.n28OnNativeLogin(idToken) / n28OnNativeLoginError(msg). */
        @JavascriptInterface
        fun googleSignIn() {
            runOnUiThread { nativeGoogleSignIn() }
        }

        @JavascriptInterface
        fun getAppBuild(): Int = UpdateManager.currentBuild(this@MainActivity)

        @JavascriptInterface
        fun getAppVersion(): String = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) { "" }

        /** Checks GitHub Releases; answers via window.n28OnUpdate({current, latest, notes, manual, error}). */
        @JavascriptInterface
        fun checkUpdate(manual: Boolean) {
            Thread {
                val cur = UpdateManager.currentBuild(this@MainActivity)
                val rel = UpdateManager.latest()
                pendingUpdateUrl = rel?.apkUrl
                val json = JSONObject().apply {
                    put("current", cur); put("latest", rel?.build ?: 0)
                    put("notes", rel?.notes ?: ""); put("manual", manual); put("error", rel == null); put("detail", UpdateManager.lastError)
                }
                call("n28OnUpdate", json.toString())
            }.start()
        }

        /** Progress via window.n28OnUpdateProgress(p): 0..100 download, 101 installer opened,
         *  -1 failed, -2 needs "install unknown apps" permission. */
        @JavascriptInterface
        fun installUpdate() {
            val url = pendingUpdateUrl ?: return call("n28OnUpdateProgress", "-1")
            if (!UpdateManager.canInstall(this@MainActivity)) {
                runOnUiThread { UpdateManager.openInstallPermissionSettings(this@MainActivity) }
                return call("n28OnUpdateProgress", "-2")
            }
            Thread {
                val apk = UpdateManager.download(this@MainActivity, url) { p -> call("n28OnUpdateProgress", p.toString()) }
                if (apk == null) { call("n28OnUpdateProgress", "-1"); return@Thread }
                runOnUiThread { UpdateManager.installSession(this@MainActivity, apk) }
                call("n28OnUpdateProgress", "101")
            }.start()
        }

        @JavascriptInterface
        fun openUpdatePage() {
            runOnUiThread {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dvaosem/notes28/releases/latest")))
            }
        }

        /** Export files from the web app (blob/data downloads): saved to Downloads, older Androids get the share sheet. */
        @JavascriptInterface
        fun saveFile(name: String, mime: String, base64: String): Boolean {
            val bytes = try { Base64.decode(base64, Base64.DEFAULT) } catch (e: Exception) { return false }
            val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifEmpty { "n28-export" }
            val type = mime.ifEmpty { "application/octet-stream" }
            return try {
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safe)
                        put(MediaStore.Downloads.MIME_TYPE, type)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
                    runOnUiThread { Toast.makeText(this@MainActivity, "Uložené do Stiahnutých: $safe", Toast.LENGTH_LONG).show() }
                } else {
                    val dir = java.io.File(cacheDir, "shared").apply { mkdirs() }
                    val f = java.io.File(dir, safe)
                    f.writeBytes(bytes)
                    val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", f)
                    runOnUiThread {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            this.type = type
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, safe))
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        /** Texts typed in the quick-note widget dialog (JSON array of strings) waiting to be written to Firestore. */
        @JavascriptInterface
        fun getInbox(): String = org.json.JSONArray(WidgetPrefs.inboxList(this@MainActivity)).toString()

        /** The web app wrote the first n inbox texts to Firestore. */
        @JavascriptInterface
        fun clearInbox(n: Int) {
            WidgetPrefs.clearInbox(this@MainActivity, n)
            NoteWidget.refreshAll(this@MainActivity)
        }

        /** Widget notes typed with a title: JSON array of {n: title, t: text}; each becomes its own note. */
        @JavascriptInterface
        fun getTitledInbox(): String = WidgetPrefs.titledJson(this@MainActivity)

        @JavascriptInterface
        fun clearTitledInbox(n: Int) {
            WidgetPrefs.clearTitled(this@MainActivity, n)
        }

        @JavascriptInterface
        fun hasWidget(): Boolean = NoteWidget.hasWidgets(this@MainActivity)

        @JavascriptInterface
        fun pinQuickWidget(): Boolean {
            val mgr = AppWidgetManager.getInstance(this@MainActivity)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            runOnUiThread {
                mgr.requestPinAppWidget(ComponentName(this@MainActivity, com.dvaosem.notes28.widget.NotesListWidget::class.java), null, null)
            }
            return true
        }

        /** Asks the launcher to pin a new widget (the config screen then lets you pick the note). */
        @JavascriptInterface
        fun pinWidget(): Boolean {
            val mgr = AppWidgetManager.getInstance(this@MainActivity)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            runOnUiThread {
                mgr.requestPinAppWidget(ComponentName(this@MainActivity, NoteWidget::class.java), null, null)
            }
            return true
        }
    }

    private fun nativeGoogleSignIn() {
        val clientId = getString(R.string.google_web_client_id)
        if (clientId.isBlank()) {
            call("n28OnNativeLoginError", JSONObject.quote("no_client_id"))
            return
        }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        lifecycleScope.launch {
            try {
                val result = CredentialManager.create(this@MainActivity).getCredential(this@MainActivity, request)
                val cred = result.credential
                if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val g = GoogleIdTokenCredential.createFrom(cred.data)
                    call("n28OnNativeLogin", JSONObject.quote(g.idToken))
                } else {
                    call("n28OnNativeLoginError", JSONObject.quote("unexpected_credential"))
                }
            } catch (e: GetCredentialException) {
                call("n28OnNativeLoginError", JSONObject.quote(e.type + ": " + (e.message ?: "")))
            } catch (e: GoogleIdTokenParsingException) {
                call("n28OnNativeLoginError", JSONObject.quote("parse: " + (e.message ?: "")))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun setupWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            setSupportMultipleWindows(false)
            userAgentString = userAgentString + " N28App"
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    fileChooser.launch(params.createIntent())
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val u = request.url
                if (u.scheme == "https" && u.host in OWN_HOSTS) return false
                try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (e: Exception) { }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                if (bootStart == 0L) bootStart = android.os.SystemClock.uptimeMillis()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (bootFinish == 0L) bootFinish = android.os.SystemClock.uptimeMillis()
                pageLoaded = true
                CookieManager.getInstance().flush()
                deliverShare()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadDataWithBaseURL(null, OFFLINE_HTML, "text/html", "utf-8", null)
                }
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("http")) {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
            } else {
                runOnUiThread { Toast.makeText(this, "Export súboru v appke zatiaľ nepodporujem", Toast.LENGTH_LONG).show() }
            }
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        js("window.n28FlushInbox&&window.n28FlushInbox()")
    }
    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
        webView.onPause()
    }
    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
