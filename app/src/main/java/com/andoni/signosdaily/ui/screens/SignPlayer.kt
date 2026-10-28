package com.andoni.signosdaily.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.TextureView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.andoni.signosdaily.data.SignRef
import com.andoni.signosdaily.media.MediaStore
import java.io.File
import java.net.URLEncoder

const val DILSE_URL = "https://fundacioncnse-dilse.org/"

/** Opens the sign in the DILSE dictionary, the source of the videos. */
fun openInDilse(context: Context, term: String) {
    val url = DILSE_URL + "?buscar=" + URLEncoder.encode(term, "UTF-8")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private sealed interface Loaded {
    data object Loading : Loaded
    data class Ready(val files: List<Pair<Int, File>>) : Loaded
    data class Failed(val missing: Boolean) : Loaded
}

/**
 * Loops the videos of [signs] one after another (an expression plays sign by sign).
 * The current sign is highlighted in the gloss underneath.
 */
@Composable
fun SignPlayer(
    signs: List<SignRef>,
    speed: Float,
    mirror: Boolean,
    onSpeedChange: (Float) -> Unit,
    onMirrorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showGloss: Boolean = true,
) {
    val context = LocalContext.current
    val media = remember { MediaStore.get(context) }
    var attempt by remember { mutableIntStateOf(0) }
    var loaded by remember(signs, attempt) { mutableStateOf<Loaded>(Loaded.Loading) }

    LaunchedEffect(signs, attempt) {
        val files = signs.mapIndexedNotNull { i, s -> s.key?.let { key -> media.video(key)?.let { i to it } } }
        loaded = if (files.isNotEmpty()) Loaded.Ready(files)
        else Loaded.Failed(missing = signs.mapNotNull { it.key }.let { keys -> keys.isNotEmpty() && keys.all(media::isMissing) })
    }

    var current by remember(signs) { mutableIntStateOf(-1) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (val state = loaded) {
            Loaded.Loading -> VideoFrame {
                CircularProgressIndicator(color = Color.White)
            }
            is Loaded.Failed -> VideoFrame {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(Icons.Outlined.CloudOff, null, tint = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (state.missing) "Este vídeo aún no está disponible."
                        else "No se ha podido cargar el vídeo. Comprueba tu conexión.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row {
                        if (!state.missing) TextButton(onClick = { attempt++ }) { Text("Reintentar", color = Color.White) }
                        TextButton(onClick = { openInDilse(context, signs.first { it.key != null }.searchTerm) }) {
                            Text("Ver en DILSE", color = Color.White)
                        }
                    }
                }
            }
            is Loaded.Ready -> LoopingVideo(
                files = state.files.map { it.second },
                speed = speed,
                mirror = mirror,
                onIndex = { playing -> current = state.files.getOrNull(playing)?.first ?: -1 },
            )
        }

        if (showGloss && signs.size > 1) GlossRow(signs, current)

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = speed < 1f,
                onClick = { onSpeedChange(if (speed < 1f) 1f else 0.5f) },
                label = { Text("Lento") },
                leadingIcon = { Icon(Icons.Outlined.SlowMotionVideo, null, Modifier.size(FilterChipDefaults.IconSize)) },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = mirror,
                onClick = { onMirrorChange(!mirror) },
                label = { Text("Espejo") },
                leadingIcon = { Icon(Icons.Outlined.Flip, null, Modifier.size(FilterChipDefaults.IconSize)) },
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Vídeo: DILSE · CNSE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { openInDilse(context, signs.firstOrNull { it.key != null }?.searchTerm ?: "") }
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun VideoFrame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B2120)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun LoopingVideo(files: List<File>, speed: Float, mirror: Boolean, onIndex: (Int) -> Unit) {
    val context = LocalContext.current
    val player = remember(files) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(files.map { MediaItem.fromUri(Uri.fromFile(it)) })
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    var aspect by remember { mutableFloatStateOf(4f / 3f) }
    var playing by remember(player) { mutableStateOf(true) }

    LaunchedEffect(player, speed) { player.setPlaybackSpeed(speed) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                onIndex(player.currentMediaItemIndex)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying || player.playWhenReady
            }
        }
        player.addListener(listener)
        onIndex(0)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Pause while the app is in the background.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (playing) player.play()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspect.coerceIn(0.5f, 2.5f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .clickable {
                if (player.playWhenReady) player.pause() else player.play()
                playing = player.playWhenReady
            },
    ) {
        key(player) {
            AndroidView(
                factory = { ctx -> TextureView(ctx).also(player::setVideoTextureView) },
                update = { it.scaleX = if (mirror) -1f else 1f },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playing) "Pausar" else "Reproducir",
                tint = Color.White,
                modifier = Modifier.padding(6.dp).size(20.dp),
            )
        }
    }
}

/** The gloss as chips, highlighting the sign that is playing. */
@Composable
private fun GlossRow(signs: List<SignRef>, current: Int) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        signs.forEachIndexed { i, sign ->
            val active = i == current
            val bg by animateColorAsState(
                when {
                    active -> MaterialTheme.colorScheme.primary
                    sign.key == null -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                label = "gloss",
            )
            Text(
                sign.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    active -> MaterialTheme.colorScheme.onPrimary
                    sign.key == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Still photo of a sign (from DILSE), used by the quiz. */
@Composable
fun SignPhoto(key: String, mirror: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val media = remember { MediaStore.get(context) }
    var image by remember(key) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        val file = media.photo(key)
        image = file?.let { runCatching { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }.getOrNull() }
        failed = image == null
    }
    Box(
        modifier
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val img = image
        when {
            img != null -> Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = if (mirror) -1f else 1f },
            )
            failed -> Icon(Icons.Outlined.CloudOff, "Sin foto", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> CircularProgressIndicator(Modifier.size(24.dp))
        }
    }
}
