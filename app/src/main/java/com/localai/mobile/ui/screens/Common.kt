package com.localai.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Decodes a base64 PNG/JPEG into a Bitmap, or null on failure. Shared helper. */
fun decode(b64: String) = try {
    val bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) { null }

/** Minimal black & white "AI" style mark — a monochrome node/brain glyph. */
@Composable
fun AiLogo(size: Int) {
    val fg = MaterialTheme.colorScheme.onBackground
    Canvas(Modifier.size(size.dp)) {
        val w = this.size.width; val h = this.size.height
        val r = w * 0.10f
        val stroke = Stroke(width = w * 0.05f)
        // central node
        drawCircle(fg, r, Offset(w / 2, h / 2))
        // three outer nodes
        val outer = listOf(Offset(w * 0.2f, h * 0.25f), Offset(w * 0.82f, h * 0.35f), Offset(w * 0.5f, h * 0.85f))
        outer.forEach {
            drawLine(fg, Offset(w / 2, h / 2), it, strokeWidth = w * 0.03f)
            drawCircle(fg, r * 0.7f, it)
        }
        drawCircle(fg, w * 0.46f, Offset(w / 2, h / 2), style = stroke)
    }
}

@Composable
fun TopBarRow(title: String, right: @Composable () -> Unit = {}, left: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        left()
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f).padding(start = 8.dp))
        right()
    }
}
