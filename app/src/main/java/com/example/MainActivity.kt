package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF064E3B))
            .statusBarsPadding()
        ) {
          MrzCardWebViewScreen()
        }
      }
    }
  }
}

/**
 * Android JavaScript Bridge exposing native capabilities (Haptics, Toast, Intent)
 * to the MRZ Card PWA.
 */
class AndroidBridge(private val context: Context) {

  @JavascriptInterface
  fun showToast(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  @JavascriptInterface
  fun vibrate(milliseconds: Long) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vibratorManager?.defaultVibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(milliseconds)
        }
      }
    } catch (_: Exception) {
      // Haptics not available or permission restricted
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MrzCardWebViewScreen() {
  val context = LocalContext.current
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }

  BackHandler(enabled = canGoBack) {
    webViewInstance?.let { webView ->
      if (webView.canGoBack()) {
        webView.goBack()
      }
    }
  }

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )

        // WebSettings configuration for rich offline PWA execution
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          databaseEnabled = true
          allowFileAccess = true
          allowContentAccess = true
          useWideViewPort = true
          loadWithOverviewMode = true
          builtInZoomControls = false
          displayZoomControls = false
          setSupportZoom(false)
          cacheMode = WebSettings.LOAD_DEFAULT
          mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Add native bridge
        addJavascriptInterface(AndroidBridge(ctx), "AndroidBridge")

        webChromeClient = object : WebChromeClient() {
          override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            canGoBack = view?.canGoBack() == true
          }
        }

        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            return handleCustomUrlSchemes(ctx, url)
          }

          @Deprecated("Deprecated in Java")
          override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            url ?: return false
            return handleCustomUrlSchemes(ctx, url)
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            canGoBack = view?.canGoBack() == true
          }
        }

        // Load the local MRZ Card PWA asset
        loadUrl("file:///android_asset/index.html")
        webViewInstance = this
      }
    },
    update = { webView ->
      webViewInstance = webView
    }
  )

  DisposableEffect(Unit) {
    onDispose {
      webViewInstance?.destroy()
    }
  }
}

/**
 * Handle custom URL schemes such as UPI (upi://pay), WhatsApp (whatsapp://, https://wa.me),
 * tel:, and mailto:.
 */
private fun handleCustomUrlSchemes(context: Context, url: String): Boolean {
  return when {
    url.startsWith("upi://") -> {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val chooser = Intent.createChooser(intent, "UPI ऐप चुनें (Pay with UPI)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
      } catch (_: Exception) {
        Toast.makeText(
          context,
          "डिवाइस पर कोई UPI ऐप (PhonePe/GPay/Paytm) नहीं मिला।",
          Toast.LENGTH_LONG
        ).show()
      }
      true
    }
    url.startsWith("whatsapp://") || url.contains("wa.me") -> {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      } catch (_: Exception) {
        Toast.makeText(
          context,
          "WhatsApp ऐप नहीं मिला।",
          Toast.LENGTH_SHORT
        ).show()
      }
      true
    }
    url.startsWith("tel:") || url.startsWith("mailto:") -> {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      } catch (_: Exception) {
        // Fallback
      }
      true
    }
    else -> false
  }
}
