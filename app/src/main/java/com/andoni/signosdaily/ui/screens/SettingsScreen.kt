package com.andoni.signosdaily.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andoni.signosdaily.BuildConfig
import com.andoni.signosdaily.ads.Ads
import com.andoni.signosdaily.data.ContentMode
import com.andoni.signosdaily.data.ThemeMode
import com.andoni.signosdaily.data.UserData
import com.andoni.signosdaily.notify.Notifications
import com.andoni.signosdaily.ui.AppViewModel

val contentModeLabels = mapOf(
    ContentMode.BOTH to ("Signo + expresión" to "Un signo y una expresión cada día. Recomendado."),
    ContentMode.WORD to ("Solo signo" to "Un signo al día. Podrás cambiarlo por la expresión cuando quieras."),
    ContentMode.EXPRESSION to ("Solo expresión" to "Una expresión al día. Podrás cambiarla por el signo cuando quieras."),
)

const val PRIVACY_POLICY_URL = "https://github.com/andonialvarez/signos_app/blob/main/PRIVACY.md"

fun formatTime(hour: Int, minute: Int) = "%02d:%02d".format(hour, minute)

@Composable
fun SettingsScreen(vm: AppViewModel, user: UserData) {
    val context = LocalContext.current
    var editingName by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val download by vm.download.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var canNotify by remember { mutableStateOf(Notifications.canPost(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = it
        if (it) vm.setNotificationsEnabled(true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        SettingsCard("Perfil") {
            Row(
                Modifier.fillMaxWidth().clickable { editingName = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tu nombre", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.name.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleMedium)
                }
                Icon(Icons.Outlined.Edit, "Editar nombre")
            }
        }

        SettingsCard("Contenido diario") {
            ContentModePicker(user.contentMode) { vm.setContentMode(it) }
        }

        SettingsCard("Notificaciones") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recordatorio diario", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Una frase motivadora distinta cada día. Si ya has estudiado, no te molesto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = user.notificationsEnabled && canNotify,
                    onCheckedChange = { on ->
                        if (on && !canNotify && Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.setNotificationsEnabled(on)
                        }
                    },
                )
            }
            if (user.notificationsEnabled) {
                Row(
                    Modifier.fillMaxWidth().clickable { pickingTime = true }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hora del recordatorio", modifier = Modifier.weight(1f))
                    Text(
                        formatTime(user.reminderHour, user.reminderMinute),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedButton(onClick = { vm.sendTestNotification() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enviar una notificación de prueba")
                }
            }
            if (!canNotify && Build.VERSION.SDK_INT >= 26) {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }) { Text("Abrir ajustes de notificaciones de Android") }
            }
        }

        SettingsCard("Vídeos") {
            Text("Velocidad", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            SpeedPicker(user.videoSpeed) { vm.setVideoSpeed(it) }
            Spacer(Modifier.height(14.dp))
            Text("Mano dominante", style = MaterialTheme.typography.titleSmall)
            Text(
                "Los vídeos están signados con la mano derecha. En modo zurdo se ven en espejo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            HandPicker(user.mirror) { vm.setMirror(it) }
        }

        SettingsCard("Usar sin conexión") {
            val keys = vm.content.allMediaKeys
            val downloaded = remember(download) { vm.media.downloadedCount(keys) }
            val pending = remember(download) { vm.media.bytesToDownload(keys) }
            Text(
                "Los signos que ya has visto se guardan en el móvil. Descarga todos para practicar sin internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$downloaded de ${keys.size} signos descargados · ${formatMb(vm.media.bytesOnDisk())} ocupados",
                style = MaterialTheme.typography.bodyMedium,
            )
            val d = download
            if (d != null && !d.finished) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (d.total == 0) 0f else d.done.toFloat() / d.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Descargando… ${d.done} de ${d.total}", style = MaterialTheme.typography.labelSmall)
            } else {
                if (d != null && d.failed > 0) {
                    Text(
                        "No se pudieron descargar ${d.failed}. Revisa tu conexión y vuelve a intentarlo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.downloadAllSigns() }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (pending != null && pending > 0) "Descargar todos (${formatMb(pending)})"
                        else "Descargar todos los signos",
                    )
                }
                if (downloaded > 0) {
                    TextButton(onClick = { confirmClear = true }) { Text("Borrar vídeos descargados") }
                }
            }
        }

        SettingsCard("Apariencia") {
            ThemePicker(user.theme) { vm.setTheme(it) }
        }

        SettingsCard("Datos") {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Reiniciar progreso", color = MaterialTheme.colorScheme.error)
            }
        }

        if (BuildConfig.SELF_UPDATE) SettingsCard("Actualizaciones") {
            val updateMessage by vm.updateMessage.collectAsStateWithLifecycle()
            Text(
                "Versión instalada: ${BuildConfig.VERSION_NAME}. La app busca versiones nuevas cada vez que la abres.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.checkForUpdates(manual = true) }, modifier = Modifier.fillMaxWidth()) {
                Text("Buscar actualizaciones")
            }
            updateMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        }

        val adPrivacyRequired by Ads.privacyOptionsRequired.collectAsStateWithLifecycle()
        if (adPrivacyRequired) SettingsCard("Anuncios") {
            Text(
                "La app es gratis gracias a los anuncios. Puedes cambiar tu consentimiento cuando quieras.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { (context as? Activity)?.let(Ads::showPrivacyOptions) }) {
                Text("Privacidad de los anuncios")
            }
        }

        SettingsCard("Acerca de") {
            Text(
                "Tu progreso se guarda solo en este móvil. " +
                    (if (BuildConfig.ADS_ENABLED) "Los anuncios los sirve Google AdMob; " else "La app no muestra anuncios; ") +
                    "más detalles en la política de privacidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_POLICY_URL)))
                }
            }) { Text("Política de privacidad") }
        }

        SettingsCard("Créditos") {
            Text(
                "Vídeos y fotos de los signos: DILSE, Diccionario de la Lengua de Signos Española, © Fundación CNSE " +
                    "para la Supresión de las Barreras de Comunicación, con licencia Creative Commons " +
                    "Reconocimiento-NoComercial-CompartirIgual 3.0. Aquí se muestran en un formato más ligero " +
                    "y se comparten con la misma licencia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Las frases signo a signo y las notas de gramática son de Signos Daily y no del diccionario. " +
                    "La LSE tiene variantes según la zona: si en tu entorno se signa distinto, sigue a tu comunidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row {
                TextButton(onClick = { openUrl(context, DILSE_URL) }) { Text("Diccionario DILSE") }
                TextButton(onClick = { openUrl(context, "https://creativecommons.org/licenses/by-nc-sa/3.0/deed.es") }) {
                    Text("Licencia")
                }
            }
        }

        Text(
            "Signos Daily ${BuildConfig.VERSION_NAME} · ${vm.content.words.size} signos y " +
                "${vm.content.expressions.size} expresiones",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }

    if (editingName) {
        var draft by remember { mutableStateOf(user.name) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("¿Cómo te llamas?") },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(30) }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { vm.setName(draft); editingName = false }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancelar") } },
        )
    }

    if (pickingTime) {
        TimePickerDialog(
            hour = user.reminderHour,
            minute = user.reminderMinute,
            onDismiss = { pickingTime = false },
            onConfirm = { h, m ->
                vm.setReminderTime(h, m)
                pickingTime = false
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar los vídeos descargados?") },
            text = { Text("Se liberará espacio en el móvil. Los signos se volverán a descargar cuando los abras con conexión.") },
            confirmButton = {
                TextButton(onClick = { vm.clearDownloads(); confirmClear = false }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Reiniciar progreso?") },
            text = { Text("Se borrarán tu racha, logros, favoritos y palabras aprendidas. Tu nombre y ajustes se mantienen.") },
            confirmButton = {
                TextButton(onClick = { vm.resetProgress(); confirmReset = false }) {
                    Text("Reiniciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ContentModePicker(selected: ContentMode, onSelect: (ContentMode) -> Unit) {
    Column {
        ContentMode.entries.forEach { mode ->
            val (title, desc) = contentModeLabels.getValue(mode)
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = mode == selected, onClick = { onSelect(mode) }, role = Role.RadioButton)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(desc, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun formatMb(bytes: Long): String = "%.1f MB".format(bytes / 1_000_000.0)

fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}

/** Right-handed (videos as recorded) or left-handed (videos mirrored). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandPicker(mirror: Boolean, onSelect: (Boolean) -> Unit) {
    val options = listOf(false to "✋ Diestro", true to "🤚 Zurdo (espejo)")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == mirror,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
            ) { Text(label) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedPicker(speed: Float, onSelect: (Float) -> Unit) {
    val options = listOf(1f to "Normal", 0.5f to "🐢 Lento")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = (speed < 1f) == (value < 1f),
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
            ) { Text(label) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val labels = mapOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Oscuro")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { i, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = ThemeMode.entries.size),
            ) { Text(labels.getValue(mode)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hora del recordatorio") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
