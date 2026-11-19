package com.personal.lira

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.personal.lira.data.DownloadItem
import com.personal.lira.ui.AppViewModel
import com.personal.lira.ui.DownloadListScreen
import com.personal.lira.ui.PlayerDetailScreen
import com.personal.lira.ui.HomeScreen
import com.personal.lira.ui.LiraTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        requestNeededPermissions()
        viewModel.acceptSharedText(intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT))

        setContent {
            LiraTheme {
                LiraApp(
                    viewModel = viewModel,
                    onOpen = ::openDownload,
                    onShare = ::shareDownload,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_SEND) {
            viewModel.acceptSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
        }
    }

    private fun requestNeededPermissions() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        }
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 42)
        }
    }

    private fun openDownload(item: DownloadItem) {
        val uri = MediaActions.uriFor(this, item) ?: return
        val type = if (item.kind == DownloadItem.KIND_AUDIO) "audio/*" else "video/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
    }

    private fun shareDownload(item: DownloadItem) {
        val uri = MediaActions.uriFor(this, item) ?: return
        val type = if (item.kind == DownloadItem.KIND_AUDIO) "audio/*" else "video/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = type
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir archivo"))
    }
}

@Composable
private fun LiraApp(
    viewModel: AppViewModel,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedDetail by remember { androidx.compose.runtime.mutableStateOf<DownloadItem?>(null) }
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val appState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(appState.homeRequest) {
        selectedTab = 0
        selectedDetail = null
    }
    val currentDetail = selectedDetail?.let { selected ->
        downloads.firstOrNull { it.id == selected.id } ?: selected
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDetail == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                        label = { Text("Inicio") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                        label = { Text("Descargas") },
                    )
                }
            }
        },
    ) { padding ->
        when {
            currentDetail != null -> PlayerDetailScreen(
                item = currentDetail,
                onBack = { selectedDetail = null },
                onOpen = onOpen,
                onShare = onShare,
                modifier = Modifier.padding(padding),
            )
            selectedTab == 0 -> HomeScreen(
                viewModel = viewModel,
                onDownloadStarted = { selectedTab = 1 },
                modifier = Modifier.padding(padding),
            )
            else -> DownloadListScreen(
                downloads = downloads,
                onSelect = { selectedDetail = it },
                onCancel = viewModel::cancelDownload,
                onOpen = onOpen,
                onShare = onShare,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
