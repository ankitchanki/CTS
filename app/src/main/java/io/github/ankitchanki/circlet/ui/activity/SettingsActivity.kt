package io.github.ankitchanki.circlet.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.ankitchanki.circlet.BuildConfig
import io.github.ankitchanki.circlet.MainApplication
import io.github.ankitchanki.circlet.R
import io.github.ankitchanki.circlet.config.AppConfig
import io.github.ankitchanki.circlet.config.Language
import io.github.ankitchanki.circlet.config.TriggerService
import io.github.ankitchanki.circlet.shizuku.ShizukuManager
import io.github.ankitchanki.circlet.shizuku.ShizukuStatus
import io.github.ankitchanki.circlet.ui.theme.CircletTheme
import io.github.ankitchanki.circlet.ui.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        viewModel = (application as MainApplication).settingsViewModel
        setContent {
            CircletTheme {
                val locale by viewModel.locale.collectAsState()
                val config = Configuration(resources.configuration).apply { setLocale(locale) }
                CompositionLocalProvider(LocalContext provides createConfigurationContext(config)) {
                    SettingsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    var showAboutDialog by remember { viewModel.showAboutDialog }

    var topAppBarState = remember { viewModel.topAppBarState }
    val appIcon = LocalContext.current.packageManager.getApplicationIcon(LocalContext.current.applicationInfo.packageName)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(BuildConfig.APP_NAME)
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { viewModel.menuExpanded }
                    var languageExpanded by remember { viewModel.languageExpanded }
                    IconButton(onClick = { menuExpanded = !menuExpanded }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                        val context = LocalContext.current
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            CompositionLocalProvider(LocalContext provides context) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language)) },
                                onClick = {
                                    menuExpanded = false
                                    languageExpanded = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about)) },
                                onClick = {
                                    menuExpanded = false
                                    showAboutDialog = true
                                }
                            )
                                }
                        }
                        DropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false },
                        ) {
                            CompositionLocalProvider(LocalContext provides context) {
                                Language.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.id)) },
                                        onClick = {
                                            viewModel.updateAppConfig(AppConfig.KEY_LANGUAGE, option.ordinal)
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }

                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        SettingsPage(modifier = Modifier.fillMaxSize().padding(paddingValues), viewModel)
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row {
                    Image(
                        painter = rememberDrawablePainter(drawable = appIcon),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(BuildConfig.APP_NAME)
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Text(buildAnnotatedString {
                    withLink(LinkAnnotation.Url(url = "https://github.com/ankitchanki/cts")) {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("https://github.com/ankitchanki/cts")
                        }
                    }
                    append("\n\nBased on ")
                    withLink(LinkAnnotation.Url(url = "https://github.com/parallelcc/micts")) {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("parallelcc/MiCTS")
                        }
                    }
                })
            },
            confirmButton = {},
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = modifier.verticalScroll(remember { viewModel.scrollState })
    ) {
        val appConfig by viewModel.appConfig.collectAsState()

        SectionHeader(stringResource(R.string.app_settings))

        SliderSettingItem(
            title = stringResource(R.string.default_trigger_delay),
            value = (appConfig[AppConfig.KEY_DEFAULT_DELAY] as Long).toFloat(),
            onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_DEFAULT_DELAY, it.toLong())},
            valueRange = 0f..2000f
        )

        SliderSettingItem(
            title = stringResource(R.string.tile_trigger_delay),
            value = (appConfig[AppConfig.KEY_TILE_DELAY] as Long).toFloat(),
            onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_TILE_DELAY, it.toLong())},
            valueRange = 0f..2000f
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.vibrate)) },
            trailingContent = {
                Switch(
                    checked = appConfig[AppConfig.KEY_VIBRATE] as Boolean,
                    onCheckedChange = {
                        viewModel.updateAppConfig(AppConfig.KEY_VIBRATE, it)
                    }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.async_trigger)) },
            trailingContent = {
                Switch(
                    checked = appConfig[AppConfig.KEY_ASYNC_TRIGGER] as Boolean,
                    onCheckedChange = {
                        viewModel.updateAppConfig(AppConfig.KEY_ASYNC_TRIGGER, it)
                    }
                )
            }
        )

        val shizukuStatus by viewModel.shizukuStatus.collectAsState()
        val canDrawOverlays by viewModel.canDrawOverlays.collectAsState()
        val assistantResult by viewModel.assistantResult.collectAsState()
        val shizukuGranted = shizukuStatus == ShizukuStatus.Granted
        val shizukuIsRoot by viewModel.shizukuIsRoot.collectAsState()
        val context = LocalContext.current

        // Full edition only: everything here needs Shizuku.
        if (BuildConfig.SHIZUKU) {
            SectionHeader(stringResource(R.string.shizuku_settings))

            ListItem(
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_shizuku),
                        contentDescription = null,
                        tint = if (shizukuGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                },
                headlineContent = {
                    Text(
                        stringResource(
                            when (shizukuStatus) {
                                ShizukuStatus.Granted -> R.string.shizuku_status_granted
                                ShizukuStatus.PermissionRequired -> R.string.shizuku_status_permission
                                ShizukuStatus.NotRunning -> R.string.shizuku_status_not_running
                                ShizukuStatus.NotInstalled -> R.string.shizuku_status_not_installed
                            }
                        )
                    )
                },
                trailingContent = {
                    when (shizukuStatus) {
                        ShizukuStatus.PermissionRequired -> TextButton(onClick = { ShizukuManager.requestPermission() }) {
                            Text(stringResource(R.string.grant))
                        }
                        ShizukuStatus.NotRunning -> TextButton(onClick = { ShizukuManager.openShizukuApp(context) }) {
                            Text(stringResource(R.string.open))
                        }
                        ShizukuStatus.NotInstalled -> TextButton(onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }) {
                            Text(stringResource(R.string.open))
                        }
                        ShizukuStatus.Granted -> Unit
                    }
                }
            )

            ListItem(
                leadingContent = { Icon(painterResource(R.drawable.ic_trigger_service), contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.system_trigger_service)) },
                trailingContent = {
                    Box {
                        var triggerServiceExpanded by remember { viewModel.triggerServiceExpanded }
                        val selectedOption = TriggerService.entries[appConfig[AppConfig.KEY_TRIGGER_SERVICE] as Int].let {
                            if (it.needsShizukuRoot && !shizukuIsRoot) "${it.name} → ${TriggerService.VIS.name}" else it.name
                        }
                        val options = TriggerService.getSupportedServices()

                        TextButton(onClick = { triggerServiceExpanded = true }) {
                            Text(text = selectedOption)
                        }

                        if (options.size <= 1) return@Box

                        DropdownMenu(
                            expanded = triggerServiceExpanded,
                            onDismissRequest = { triggerServiceExpanded = false }
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    enabled = !option.needsShizukuRoot || shizukuIsRoot,
                                    onClick = {
                                        triggerServiceExpanded = false
                                        viewModel.updateAppConfig(AppConfig.KEY_TRIGGER_SERVICE, option.ordinal)
                                    }
                                )
                            }
                        }
                    }
                }
            )

            ListItem(
                leadingContent = { Icon(painterResource(R.drawable.ic_assistant), contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.set_google_assistant)) },
                supportingContent = {
                    Text(
                        when (assistantResult) {
                            true -> stringResource(R.string.done)
                            false -> stringResource(R.string.failed)
                            null -> stringResource(if (shizukuGranted) R.string.set_google_assistant_summary else R.string.requires_shizuku)
                        }
                    )
                },
                trailingContent = {
                    TextButton(enabled = shizukuGranted, onClick = { viewModel.setGoogleAsAssistant() }) {
                        Text(stringResource(R.string.grant))
                    }
                }
            )
        }

        SectionHeader(stringResource(R.string.gesture_settings))

        ListItem(
            leadingContent = { Icon(painterResource(R.drawable.ic_corner_gesture), contentDescription = null) },
            headlineContent = { Text(stringResource(R.string.corner_gesture)) },
            supportingContent = {
                Text(
                    stringResource(
                        if (appConfig[AppConfig.KEY_CORNER_GESTURE] as Boolean && !canDrawOverlays) {
                            R.string.overlay_permission_required
                        } else {
                            R.string.corner_gesture_summary
                        }
                    )
                )
            },
            trailingContent = {
                Switch(
                    checked = appConfig[AppConfig.KEY_CORNER_GESTURE] as Boolean,
                    onCheckedChange = { enabled ->
                        viewModel.setCornerGesture(enabled) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
        )

        if (appConfig[AppConfig.KEY_CORNER_GESTURE] as Boolean && canDrawOverlays) {
            CornerGestureGuide(onShowMe = { viewModel.showCornerHint() })
        }

        // Full edition only.
        if (BuildConfig.SHIZUKU) {
            ListItem(
                leadingContent = { Icon(painterResource(R.drawable.ic_assistant), contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.block_assist_gesture)) },
                supportingContent = {
                    Text(stringResource(if (shizukuGranted) R.string.block_assist_gesture_summary else R.string.requires_shizuku))
                },
                trailingContent = {
                    Switch(
                        enabled = shizukuGranted,
                        checked = appConfig[AppConfig.KEY_BLOCK_ASSIST_GESTURE] as Boolean,
                        onCheckedChange = { viewModel.updateAppConfig(AppConfig.KEY_BLOCK_ASSIST_GESTURE, it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.find_assist_settings)) },
                supportingContent = {
                    Text(stringResource(if (shizukuGranted) R.string.find_assist_settings_summary else R.string.requires_shizuku))
                },
                trailingContent = {
                    TextButton(enabled = shizukuGranted, onClick = { viewModel.findAssistGestureSettings() }) {
                        Text(stringResource(R.string.find))
                    }
                }
            )
        }

    }

    val report by viewModel.assistSettingsReport.collectAsState()
    report?.let { lines -> AssistSettingsReportDialog(lines) { viewModel.dismissAssistSettingsReport() } }
}

/** Shows what the assistant-gesture settings search found, with a button to copy it. */
@Composable
fun AssistSettingsReportDialog(lines: List<String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = if (lines.isEmpty()) stringResource(R.string.find_assist_settings_none) else lines.joinToString("\n")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.find_assist_settings)) },
        text = {
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("Circlet assistant settings", text))
            }) {
                Text(stringResource(R.string.copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
fun SectionHeader(title: String) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
fun SliderSettingItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${value.toInt()} ms",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                textAlign = TextAlign.End
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 39
        )
    }
}

/** Explains where the invisible corner zones are, with a diagram and a button that flashes them on screen. */
@Composable
fun CornerGestureGuide(onShowMe: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CornerGestureDiagram(Modifier.size(width = 78.dp, height = 140.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.corner_guide_title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.corner_guide_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onShowMe) {
                    Text(stringResource(R.string.corner_guide_show_me))
                }
            }
        }
    }
}

/** Mini phone: gesture line at the bottom, the two corner zones just above it, arrows inward and up. */
@Composable
fun CornerGestureDiagram(modifier: Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val handle = MaterialTheme.colorScheme.onSurfaceVariant
    val zone = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawRoundRect(outline, cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(stroke))
        // Gesture line, then the strip the system owns.
        val handleY = h - 7.dp.toPx()
        drawLine(handle, Offset(w * 0.35f, handleY), Offset(w * 0.65f, handleY), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        val zoneSize = 16.dp.toPx()
        val zoneBottom = h - 14.dp.toPx()
        val inset = 4.dp.toPx()
        listOf(true, false).forEach { left ->
            val x = if (left) inset else w - inset - zoneSize
            drawRoundRect(zone.copy(alpha = 0.25f), Offset(x, zoneBottom - zoneSize), Size(zoneSize, zoneSize), CornerRadius(4.dp.toPx()))
            drawRoundRect(zone, Offset(x, zoneBottom - zoneSize), Size(zoneSize, zoneSize), CornerRadius(4.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            // Arrow from the zone towards the centre, about 45 degrees.
            val start = Offset(x + zoneSize / 2, zoneBottom - zoneSize / 2)
            val len = w * 0.34f
            val end = Offset(start.x + if (left) len else -len, start.y - len)
            val head = 7.dp.toPx()
            val path = Path().apply {
                moveTo(start.x, start.y); lineTo(end.x, end.y)
                moveTo(end.x, end.y); lineTo(end.x + if (left) -head else head, end.y)
                moveTo(end.x, end.y); lineTo(end.x, end.y + head)
            }
            drawPath(path, zone, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

