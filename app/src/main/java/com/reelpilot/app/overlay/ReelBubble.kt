package com.reelpilot.app.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reelpilot.app.manager.ScrollState

@Composable
fun ReelBubble(
    remaining: Int,
    total: Int,
    state: ScrollState,
    pauseReason: String?,
    onPauseResume: () -> Unit,
    onSnooze: () -> Unit,
    onStop: () -> Unit
) {
    val progress = if (total > 0) (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            Modifier.padding(12.dp).widthIn(min = 148.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 7.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (state) {
                            ScrollState.RUNNING -> "${remaining}s"
                            ScrollState.PAUSED -> "⏸ ${remaining}s"
                            ScrollState.IDLE -> "○"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        when (state) {
                            ScrollState.RUNNING -> "next scroll"
                            ScrollState.PAUSED -> (pauseReason ?: "paused").take(18)
                            ScrollState.IDLE -> "stopped"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(onClick = onPauseResume, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(if (state == ScrollState.RUNNING) "⏸" else "▶")
                }
                FilledTonalButton(onClick = onSnooze, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("+10s")
                }
                FilledTonalButton(onClick = onStop, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("⏹")
                }
            }
        }
    }
}
