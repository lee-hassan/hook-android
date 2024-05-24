package com.hanto.hook.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.hanto.hook.BuildConfig
import com.hanto.hook.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class LoginWebViewActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_URL = "EXTRA_URL"
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val webView = findViewById<WebView>(R.id.mainWebView)
        val initURL = intent.getStringExtra(EXTRA_URL) ?: return

        webView.apply {
            settings.javaScriptEnabled = true

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url.toString()
                    if (url.startsWith(BuildConfig.KAKAO_REDIRECT)) {
                        handleRedirect(url)
                        return true
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
            loadUrl(initURL)
        }
    }

    private fun handleRedirect(url: String) {
        lifecycleScope.launch {
            try {
                val response = URL(url).readText()
                val jsonObject = JSONObject(response)
                val accessToken = jsonObject.getString("accessToken")
                val refreshToken = jsonObject.getString("refreshToken")

                saveToken(accessToken, refreshToken)

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@LoginWebViewActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveToken(accessToken: String, refreshToken: String) {
        val accessTokenKey = stringPreferencesKey("access_token")
        val refreshTokenKey = stringPreferencesKey("refresh_token")

        applicationContext.dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
        }
    }
}