package dev.minimalphone.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.minimalphone.ui.AppIcon
import dev.minimalphone.ui.theme.LocalMinimalPalette

@Composable
fun NativeAppWidgetCard(
    appWidgetId: Int,
    helper: AppWidgetHostHelper,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current
    val info = remember(appWidgetId) { helper.appWidgetManager.getAppWidgetInfo(appWidgetId) }

    if (info == null) {
        return
    }

    val widgetLabel = remember(info) {
        runCatching { info.loadLabel(context.packageManager) }.getOrDefault("Widget")
    }
    val appLabel = remember(info) {
        runCatching {
            val appInfo = context.packageManager.getApplicationInfo(info.provider.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(info.provider.packageName)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, palette.subtleBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    AppIcon(packageName = info.provider.packageName, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$appLabel · $widgetLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove widget",
                        tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            AndroidView(
                factory = { ctx ->
                    val hostView = helper.appWidgetHost.createView(ctx, appWidgetId, info)
                    hostView.setAppWidget(appWidgetId, info)
                    hostView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove widget?") },
            text = { Text("Remove $widgetLabel from your home widgets?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onRemove()
                    },
                ) {
                    Text("Remove", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetBottomSheet(
    helper: AppWidgetHostHelper,
    onDismiss: () -> Unit,
    onWidgetAdded: (Int) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current
    val pm = context.packageManager

    var searchQuery by remember { mutableStateOf("") }
    var pendingWidgetId by remember { mutableIntStateOf(-1) }
    var pendingProviderInfo by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }

    val allProviders = remember { helper.getInstalledProviders() }

    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) {
            helper.addWidgetId(pendingWidgetId)
            onWidgetAdded(pendingWidgetId)
            onDismiss()
        } else if (pendingWidgetId != -1) {
            helper.deleteWidget(pendingWidgetId)
        }
        pendingWidgetId = -1
        pendingProviderInfo = null
    }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingWidgetId
        val info = pendingProviderInfo
        if (result.resultCode == Activity.RESULT_OK && id != -1 && info != null) {
            if (info.configure != null) {
                val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
                runCatching { configureLauncher.launch(configIntent) }
            } else {
                helper.addWidgetId(id)
                onWidgetAdded(id)
                onDismiss()
                pendingWidgetId = -1
                pendingProviderInfo = null
            }
        } else if (id != -1) {
            helper.deleteWidget(id)
            pendingWidgetId = -1
            pendingProviderInfo = null
        }
    }

    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == Activity.RESULT_OK && id != -1) {
            val info = helper.appWidgetManager.getAppWidgetInfo(id)
            if (info?.configure != null) {
                pendingWidgetId = id
                pendingProviderInfo = info
                val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
                runCatching { configureLauncher.launch(configIntent) }
            } else {
                helper.addWidgetId(id)
                onWidgetAdded(id)
                onDismiss()
            }
        } else if (pendingWidgetId != -1) {
            helper.deleteWidget(pendingWidgetId)
            pendingWidgetId = -1
        }
    }

    val handleSelectProvider: (AppWidgetProviderInfo) -> Unit = { providerInfo ->
        val widgetId = helper.allocateWidgetId()
        pendingWidgetId = widgetId
        pendingProviderInfo = providerInfo

        val canBind = helper.bindWidget(widgetId, providerInfo.provider)
        if (!canBind) {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            }
            runCatching {
                bindLauncher.launch(bindIntent)
            }.onFailure {
                helper.deleteWidget(widgetId)
                pendingWidgetId = -1
                pendingProviderInfo = null
            }
        } else {
            if (providerInfo.configure != null) {
                val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = providerInfo.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                runCatching {
                    configureLauncher.launch(configIntent)
                }.onFailure {
                    helper.addWidgetId(widgetId)
                    onWidgetAdded(widgetId)
                    onDismiss()
                    pendingWidgetId = -1
                    pendingProviderInfo = null
                }
            } else {
                helper.addWidgetId(widgetId)
                onWidgetAdded(widgetId)
                onDismiss()
                pendingWidgetId = -1
                pendingProviderInfo = null
            }
        }
    }

    val filteredProviders = remember(allProviders, searchQuery) {
        if (searchQuery.isBlank()) {
            allProviders
        } else {
            allProviders.filter { info ->
                val label = runCatching { info.loadLabel(pm) }.getOrDefault("")
                val pkg = info.provider.packageName
                label.contains(searchQuery, ignoreCase = true) || pkg.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxHeight(0.85f),
        ) {
            Text(
                text = "Add Native App Widget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                text = "Built by your installed apps (Google Calendar, Samsung Health, Google Fit, Spotify, etc.)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                onClick = {
                    val allocatedId = helper.allocateWidgetId()
                    pendingWidgetId = allocatedId
                    val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
                    }
                    runCatching { systemPickerLauncher.launch(pickIntent) }
                },
                shape = RoundedCornerShape(16.dp),
                color = colors.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "System picker",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Browse Android System Widget Picker",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimaryContainer,
                        )
                        Text(
                            text = "Standard Android dialog with all app previews",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed widgets (${allProviders.size})...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.25f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredProviders, key = { "${it.provider.flattenToString()}_${it.loadLabel(pm)}" }) { info ->
                    val pkg = info.provider.packageName
                    val widgetLabel = runCatching { info.loadLabel(pm) }.getOrDefault("Widget")
                    val appLabel = runCatching {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(pkg)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, palette.subtleBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { handleSelectProvider(info) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(packageName = pkg, size = 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = widgetLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = appLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
