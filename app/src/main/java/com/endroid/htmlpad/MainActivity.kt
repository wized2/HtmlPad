package com.endroid.htmlpad

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HtmlPadApp()
        }
    }
}

private val Paper = Color(0xFFFFFBF5)
private val Ink = Color(0xFF1C1917)
private val Rule = Color(0xFFE7E0D6)
private val Accent = Color(0xFFEA580C)
private val Bar = Color(0xFFF5EFE6)

@Composable
fun HtmlPadApp() {
    val activity = LocalContext.current as? Activity
    var landscape by remember { mutableStateOf(false) }
    var value by remember {
        mutableStateOf(
            TextFieldValue(
                text = "<!DOCTYPE html>\n<html>\n<head>\n  <title></title>\n</head>\n<body>\n  \n</body>\n</html>\n",
                selection = TextRange(62),
            )
        )
    }

    fun insertSnippet(snippet: String) {
        val t = value.text
        val start = value.selection.min
        val end = value.selection.max
        val next = t.substring(0, start) + snippet + t.substring(end)
        val cursor = start + snippet.length
        value = TextFieldValue(next, TextRange(cursor))
    }

    fun onTextChange(newValue: TextFieldValue) {
        val old = value
        val oldText = old.text
        val newText = newValue.text
        // Detect single-character insert at cursor for auto-close
        if (newText.length == oldText.length + 1 && newValue.selection.collapsed) {
            val pos = newValue.selection.start
            if (pos > 0) {
                val inserted = newText.substring(pos - 1, pos)
                val beforeCursorOld = pos - 1
                // Reconstruct "before insert" state
                val before = oldText
                // Only auto-handle when the insert is '>'
                if (inserted == ">") {
                    val (result, cursor) = HtmlTagEngine.afterInsert(before, beforeCursorOld, ">")
                    value = TextFieldValue(result, TextRange(cursor))
                    return
                }
            }
        }
        value = newValue
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Title bar — notepad style
        Row(
            Modifier
                .fillMaxWidth()
                .background(Bar)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "HtmlPad",
                color = Ink,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    landscape = !landscape
                    activity?.requestedOrientation =
                        if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                },
            ) {
                Icon(Icons.Default.ScreenRotation, contentDescription = "Toggle landscape", tint = Ink)
            }
            IconButton(
                onClick = {
                    value = TextFieldValue("")
                },
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Ink)
            }
        }

        // Editor
        BasicTextField(
            value = value,
            onValueChange = { onTextChange(it) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            textStyle = TextStyle(
                color = Ink,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily.Monospace,
            ),
            cursorBrush = SolidColor(Accent),
        )

        // Symbol bar above keyboard
        SymbolBar(
            onInsert = { insertSnippet(it) },
        )
    }
}

@Composable
private fun SymbolBar(onInsert: (String) -> Unit) {
    val symbols = listOf(
        "!" to "!",
        "<" to "<",
        ">" to ">",
        "</" to "</",
        "/>" to " />",
        "\"" to "\"",
        "/" to "/",
        "=" to "=",
        "&" to "&",
        "div" to "<div></div>",
        "p" to "<p></p>",
        "h1" to "<h1></h1>",
        "h2" to "<h2></h2>",
        "br" to "<br>",
        "a" to "<a href=\"\"></a>",
        "img" to "<img src=\"\" alt=\"\">",
    )
    Surface(
        color = Bar,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            symbols.forEach { (label, snippet) ->
                TextButton(
                    onClick = { onInsert(snippet) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(label, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
            }
        }
    }
}
