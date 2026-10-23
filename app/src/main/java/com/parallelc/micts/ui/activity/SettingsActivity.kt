package com.parallelc.micts.ui.activity

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.MainApplication
import com.parallelc.micts.R
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.GestureAction
import com.parallelc.micts.config.Language
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.shizuku.ShizukuManager
import com.parallelc.micts.shizuku.ShizukuStatus
import com.parallelc.micts.ui.theme.MiCTSTheme
import com.parallelc.micts.ui.viewmodel.SettingsViewModel

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
            MiCTSTheme {
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
        val context = LocalContext.current

        SectionHeader(stringResource(R.string.shizuku_settings))

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = if (shizukuGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
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
            headlineContent = { Text(stringResource(R.string.system_trigger_service)) },
            trailingContent = {
                Box {
                    var triggerServiceExpanded by remember { viewModel.triggerServiceExpanded }
                    val selectedOption = TriggerService.entries[appConfig[AppConfig.KEY_TRIGGER_SERVICE] as Int].name
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
                                enabled = !option.needsShizuku || shizukuGranted,
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

        SectionHeader(stringResource(R.string.gesture_settings))

        ListItem(
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

        ListItem(
            headlineContent = { Text(stringResource(R.string.gesture_action)) },
            supportingContent = { if (!shizukuGranted) Text(stringResource(R.string.requires_shizuku)) },
            trailingContent = {
                Box {
                    var gestureActionExpanded by remember { viewModel.gestureActionExpanded }
                    val selected = GestureAction.entries[appConfig[AppConfig.KEY_GESTURE_ACTION] as Int]

                    TextButton(onClick = { gestureActionExpanded = true }) {
                        Text(text = stringResource(selected.id))
                    }

                    DropdownMenu(
                        expanded = gestureActionExpanded,
                        onDismissRequest = { gestureActionExpanded = false }
                    ) {
                        GestureAction.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.id)) },
                                onClick = {
                                    gestureActionExpanded = false
                                    viewModel.updateAppConfig(AppConfig.KEY_GESTURE_ACTION, option.ordinal)
                                }
                            )
                        }
                    }
                }
            }
        )
    }
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
