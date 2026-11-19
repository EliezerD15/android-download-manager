package com.personal.lira.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.personal.lira.data.DownloadItem

@Composable
fun DownloadListScreen(
    downloads: List<DownloadItem>,
    onSelect: (DownloadItem) -> Unit,
    onCancel: (DownloadItem) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(
                "Descargas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${downloads.count { it.status == DownloadItem.STATUS_COMPLETED }} completadas · " +
                    "${downloads.count { it.status == DownloadItem.STATUS_DOWNLOADING || it.status == DownloadItem.STATUS_QUEUED }} activas",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (downloads.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("Todavía no hay descargas", modifier = Modifier.padding(top = 14.dp), fontWeight = FontWeight.SemiBold)
                Text(
                    "Analiza un enlace para elegir video o audio.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadCard(item, onSelect, onCancel, onOpen, onShare)
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onSelect: (DownloadItem) -> Unit,
    onCancel: (DownloadItem) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
) {
    val active = item.status == DownloadItem.STATUS_QUEUED ||
        item.status == DownloadItem.STATUS_DOWNLOADING
    val completed = item.status == DownloadItem.STATUS_COMPLETED
    Card(
        onClick = { onSelect(item) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, maxLines = 2, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${item.quality} · ${item.extension.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        statusLabel(item),
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            completed -> MaterialTheme.colorScheme.primary
                            item.status == DownloadItem.STATUS_FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (active) {
                    TextButton(onClick = { onCancel(item) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancelar")
                    }
                }
            }
            if (active) {
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${item.progress}%", style = MaterialTheme.typography.labelSmall)
            }
            item.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (completed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpen(item) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Abrir")
                    }
                    OutlinedButton(onClick = { onShare(item) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Compartir")
                    }
                    if (item.kind == DownloadItem.KIND_AUDIO) {
                        OutlinedButton(onClick = { onSelect(item) }) {
                            Icon(Icons.Outlined.MusicNote, contentDescription = "Ver letra")
                        }
                    }
                }
            }
        }
    }
}

private fun statusLabel(item: DownloadItem): String = when (item.status) {
    DownloadItem.STATUS_QUEUED -> "En cola"
    DownloadItem.STATUS_DOWNLOADING -> "Descargando"
    DownloadItem.STATUS_COMPLETED -> "Completada"
    DownloadItem.STATUS_FAILED -> "No se pudo completar"
    DownloadItem.STATUS_CANCELLED -> "Cancelada"
    else -> item.status
}
