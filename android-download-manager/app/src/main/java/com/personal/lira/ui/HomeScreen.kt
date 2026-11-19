package com.personal.lira.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.personal.lira.data.DownloadItem
import com.personal.lira.data.MediaDetails
import com.personal.lira.data.MediaOption

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onDownloadStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val media = state.media
    val selected = state.selectedOption

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 22.dp,
            end = 20.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Tu biblioteca empieza aquí",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Pega un enlace y elige cómo guardarlo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::setUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enlace multimedia") },
                placeholder = { Text("https://…") },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Link, null) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(18.dp),
            )
        }
        item {
            Button(
                onClick = viewModel::analyze,
                enabled = !state.isAnalyzing && state.url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(10.dp))
                    Text("Analizando enlace…")
                } else {
                    androidx.compose.material3.Icon(Icons.Outlined.Analytics, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Analizar")
                }
            }
        }
        state.errorMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::clearError) { Text("Cerrar") }
                    }
                }
            }
        }
        if (media != null) {
            item { MediaSummary(media) }
            item { OptionHeading("VIDEO", "MP4", Icons.Outlined.Movie) }
            items(media.options.filter { it.kind == DownloadItem.KIND_VIDEO }) { option ->
                OptionRow(
                    option = option,
                    selected = selected?.id == option.id,
                    onClick = { viewModel.selectOption(option) },
                )
            }
            item { OptionHeading("AUDIO", "MP3 · M4A", Icons.Outlined.MusicNote) }
            items(media.options.filter { it.kind == DownloadItem.KIND_AUDIO }) { option ->
                OptionRow(
                    option = option,
                    selected = selected?.id == option.id,
                    onClick = { viewModel.selectOption(option) },
                )
            }
            item {
                Button(
                    onClick = { viewModel.startDownload(onDownloadStarted) },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Añadir a descargas")
                }
            }
        }
    }
}

@Composable
private fun MediaSummary(media: MediaDetails) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = media.thumbnailUrl,
                contentDescription = "Miniatura de ${media.title}",
                modifier = Modifier.size(88.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    media.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(media.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (media.durationSeconds > 0) {
                    Text(
                        "Duración · ${formatDuration(media.durationSeconds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionHeading(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.padding(start = 10.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OptionRow(
    option: MediaOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.dp else 0.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(option.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                option.estimatedBytes?.let(::formatFileSize) ?: "Tamaño variable",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "%d:%02d".format(minutes, remainder)
}

internal fun formatFileSize(bytes: Long): String {
    if (bytes < 1_000_000L) return "${(bytes / 1_000L).coerceAtLeast(1)} KB"
    return "%.1f MB".format(bytes / 1_000_000.0)
}
