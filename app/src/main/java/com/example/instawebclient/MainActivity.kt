package com.example.instawebclient

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ContextMenu
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.example.instawebclient.databinding.ActivityMainBinding
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentMediaUrl: String? = null
    private var desktopMode = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            currentMediaUrl?.let { downloadMedia(it) }
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        desktopMode = prefs.getBoolean("desktop_mode", false)

        setupWebView()
        setupFabs()

        binding.webView.loadUrl("https://www.instagram.com/")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        if (desktopMode) {
            settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return if (url.contains("instagram.com")) {
                    false
                } else {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) {}
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHelpers()
            }
        }

        webView.webChromeClient = WebChromeClient()

        registerForContextMenu(webView)
        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            when (result.type) {
                WebView.HitTestResult.IMAGE_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    val imgUrl = result.extra
                    if (!imgUrl.isNullOrBlank()) {
                        showDownloadDialog(imgUrl)
                        true
                    } else false
                }
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    val link = result.extra
                    if (!link.isNullOrBlank() && (link.contains(".mp4") || link.contains("video"))) {
                        showDownloadDialog(link)
                        true
                    } else false
                }
                else -> false
            }
        }
    }

    private fun injectHelpers() {
        val js = """
            (function() {
                window.__instaMedia = window.__instaMedia || [];
                function collect() {
                    const medias = [];
                    document.querySelectorAll('img, video, source').forEach(el => {
                        let src = el.src || el.currentSrc || el.getAttribute('src');
                        if (src && (src.includes('cdninstagram') || src.includes('fbcdn') || src.includes('scontent'))) {
                            medias.push(src);
                        }
                    });
                    window.__instaMedia = [...new Set(medias)];
                }
                collect();
                setInterval(collect, 2500);
            })();
        """.trimIndent()
        binding.webView.evaluateJavascript(js, null)
    }

    private fun setupFabs() {
        binding.fabDownload.setOnClickListener {
            tryDownloadCurrentMedia()
        }

        binding.fabMenu.setOnClickListener {
            showMenu()
        }
    }

    private fun showMenu() {
        val options = arrayOf("Home", "Refresh", "Settings", "Clear Cache", "Toggle Desktop Mode")
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> binding.webView.loadUrl("https://www.instagram.com/")
                    1 -> binding.webView.reload()
                    2
