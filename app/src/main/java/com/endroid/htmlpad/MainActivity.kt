package com.endroid.htmlpad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var store: DraftStore
    private var latestText: String? = null
    private var latestCursor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = DraftStore(this)
        val initial = store.load()
        setContent {
            HtmlPadApp(
                initial = initial,
                onChange = { text, cursor ->
                    latestText = text
                    latestCursor = cursor
                },
                onSave = { text, cursor -> store.save(text, cursor) },
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Make sure nothing typed in the last moments is lost.
        latestText?.let { store.save(it, latestCursor) }
    }
}

private val Paper = Color(0xFFFFFBF5)
private val Ink = Color(0xFF1C1917)
private val Accent = Color(0xFFEA580C)
private val Bar = Color(0xFFF5EFE6)

private const val STARTER = "<!DOCTYPE html>\n<html>\n<head>\n  <title></title>\n</head>\n<body>\n  \n</body>\n</html>\n"
private const val MAX_SHARE_CHARS = 200_000
private const val AUTOSAVE_DELAY_MS = 600L

/** Caret position on the empty indented line inside <body> of [STARTER]. */
private fun starterCursor(): Int = STARTER.indexOf("  \n</body>") + 2

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
fun HtmlPadApp(
    initial: Draft?,
    onChange: (String, Int) -> Unit,
    onSave: (String, Int) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var landscape by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var value by remember {
        mutableStateOf(
            if (initial != null) {
                TextFieldValue(initial.text, TextRange(initial.cursor))
            } else {
                TextFieldValue(STARTER, TextRange(starterCursor()))
            },
        )
    }

    // Autosave: remember the latest text right away, write it after a short pause.
    LaunchedEffect(value.text, value.selection) {
        onChange(value.text, value.selection.start)
        delay(AUTOSAVE_DELAY_MS)
        onSave(value.text, value.selection.start)
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val text = FileIo.read(context, uri)
            if (text == null) {
                toast(context, "Couldn't open that file (too large or unreadable)")
            } else {
                value = TextFieldValue(text, TextRange(0))
                currentUri = uri
                fileName = FileIo.displayName(context, uri)
            }
        }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri: Uri? ->
        if (uri != null) {
            if (FileIo.write(context, uri, value.text)) {
                currentUri = uri
                fileName = FileIo.displayName(context, uri)
                toast(context, "Saved")
            } else {
                toast(context, "Couldn't save the file")
            }
        }
    }

    fun saveAs() {
        saveAsLauncher.launch(fileName ?: "untitled.html")
    }

    fun save() {
        val uri = currentUri
        if (uri != null && FileIo.write(context, uri, value.text)) {
            toast(context, "Saved")
        } else {
            saveAs()
        }
    }

    fun share() {
        if (value.text.length > MAX_SHARE_CHARS) {
            toast(context, "Too large to share as text, use Save instead")
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value.text)
            putExtra(Intent.EXTRA_SUBJECT, fileName ?: "HtmlPad")
        }
        context.startActivity(Intent.createChooser(send, "Share HTML"))
    }

    fun insertSnippet(snippet: Snippet) {
        val t = value.text
        val start = value.selection.min
        val end = value.selection.max
        val next = t.substring(0, start) + snippet.text + t.substring(end)
        value = TextFieldValue(next, TextRange(start + snippet.cursor))
    }

    fun onTextChange(newValue: TextFieldValue) {
        val oldText = value.text
        val newText = newValue.text
        // A single '>' typed at the caret may auto-close the tag.
        if (newText.length == oldText.length + 1 && newValue.selection.collapsed) {
            val pos = newValue.selection.start
            if (pos > 0 && newText[pos - 1] == '>' &&
                newText.substring(0, pos - 1) + newText.substring(pos) == oldText
            ) {
                val (result, cursor) = HtmlTagEngine.afterInsert(oldText, pos - 1, ">")
                value = TextFieldValue(result, TextRange(cursor))
                return
            }
        }
        value = newValue
    }

    if (showPreview) {
        PreviewScreen(html = value.text, onClose = { showPreview = false })
    } else {
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
                    fileName ?: "HtmlPad",
                    color = Ink,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(onClick = { showPreview = true }) {
                    Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = Ink)
                }
                IconButton(onClick = { share() }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Ink)
                }
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
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Ink)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Open…") },
                            onClick = {
                                menuOpen = false
                                openLauncher.launch(arrayOf("*/*"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save") },
                            onClick = {
                                menuOpen = false
                                save()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save as…") },
                            onClick = {
                                menuOpen = false
                                saveAs()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Clear") },
                            onClick = {
                                menuOpen = false
                                confirmClear = true
                            },
                        )
                    }
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
            SymbolBar(onInsert = { insertSnippet(it) })
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear everything?") },
            text = { Text("This removes all the text in the editor. The opened file on your device is not changed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        value = TextFieldValue("")
                        // Detach from the opened file so Save can't overwrite it with nothing.
                        currentUri = null
                        fileName = null
                        confirmClear = false
                    },
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PreviewScreen(html: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Bar)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to editor", tint = Ink)
            }
            Text(
                "Preview",
                color = Ink,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = WebViewClient()
                }
            },
            update = { web ->
                if (web.tag != html) {
                    web.tag = html
                    web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            },
            onRelease = { web -> web.destroy() },
        )
    }
}

@Composable
private fun SymbolBar(onInsert: (Snippet) -> Unit) {
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
            Snippets.all.forEach { snippet ->
                TextButton(
                    onClick = { onInsert(snippet) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(snippet.label, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
            }
        }
    }
}
