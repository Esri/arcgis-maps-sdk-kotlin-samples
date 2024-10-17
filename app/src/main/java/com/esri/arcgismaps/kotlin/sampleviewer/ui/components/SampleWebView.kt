/* Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgismaps.kotlin.sampleviewer.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebViewer to display formatted .kt code files.
 */
@Composable
fun CodeView(code: String) {
    if (LocalInspectionMode.current) {
        return // if composition is composed inside a preview component
    }

    val isDeviceInDarkMode = isSystemInDarkTheme()
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        factory = { ctx -> createWebView(ctx) },
        update = { webView ->
            webView.loadDataWithBaseURL(
                /* baseUrl = */ "file:///android_asset/www/highlight/",
                /* data = */ formatWebViewHTMLContent(
                    rawFileContents = code,
                    webViewType = WebViewType.CODE_VIEW,
                    isDeviceInDarkMode = isDeviceInDarkMode
                ),
                /* mimeType = */ "text/html",
                /* encoding = */ "utf-8",
                /* historyUrl = */ null
            )
        }
    )
}

/**
 * WebViewer to display README.md files.
 */
@Composable
fun ReadmeView(
    markdownText: String
) {
    if (LocalInspectionMode.current) {
        return // if composition is composed inside a preview component
    }

    val isDeviceInDarkMode = isSystemInDarkTheme()
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        factory = { ctx ->
            createWebView(ctx).apply {
                loadDataWithBaseURL(
                    /* baseUrl = */ "file:///android_asset/www/highlight/",
                    /* data = */ formatWebViewHTMLContent(
                        rawFileContents = markdownText,
                        webViewType = WebViewType.README_VIEW,
                        isDeviceInDarkMode = isDeviceInDarkMode
                    ),
                    /* mimeType = */ "text/html",
                    /* encoding = */ "utf-8",
                    /* historyUrl = */ null
                )
            }
        }
    )
}

/**
 * Set up the HTML [String] to be displayed based on the given [rawFileContents]
 * given the [webViewType].
 */
fun formatWebViewHTMLContent(
    rawFileContents: String,
    webViewType: WebViewType,
    isDeviceInDarkMode: Boolean
): String {
    val webViewHTML = if (webViewType == WebViewType.README_VIEW) {
        readmeHTML.replace("\\(content)", rawFileContents)
    } else {
        if (!isDeviceInDarkMode) {
            codeViewHTML
                .replace("github-dark", "github")
                .replace("\\(content)", rawFileContents)
        } else {
            codeViewHTML.replace("\\(content)", rawFileContents)
        }
    }
    return webViewHTML
}

/**
 * Create the [WebView] using the default settings to load the static html.
 */
@SuppressLint("SetJavaScriptEnabled")
fun createWebView(context: Context): WebView {
    return WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        alpha = 0.99f // TODO: Weirdly, without this the screen crashes on popBackStack (#4632)
    }
}

enum class WebViewType {
    README_VIEW,
    CODE_VIEW
}

val codeViewHTML: String = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <!-- GitHub highlight JS -->
        <link rel="stylesheet" href="styles/github-dark.css">
    </head>
    <body>
        <!-- Set global margin & padding = 0 -->
        <style>* { margin: 0;padding: 0; }
        </style>
        <script src="highlight.min.js"></script>
        <!-- Kotlin highlight JS -->
        <script src="languages/kotlin.min.js"></script>
        <script>hljs.highlightAll();</script>
        <pre><code class="language-kotlin">\(content)</code></pre>
    </body>
    </html>
""".trimIndent()

val readmeHTML: String = """
    <!DOCTYPE html>
    <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="styles/info.css">
        </head>
        <body>
            <div id="preview" sd-model-to-html="text">
                <div id="content">\(content)</div>
            </div>
            <script src="showdown.min.js">
            </script>
            <script>
                var conv = new showdown.Converter();
                var text = document.getElementById('content').innerHTML;
                document.getElementById('content').innerHTML = conv.makeHtml(text);
            </script>
        </body>
    </html>    
""".trimIndent()
