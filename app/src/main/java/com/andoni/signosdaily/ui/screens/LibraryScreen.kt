package com.andoni.signosdaily.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andoni.signosdaily.data.Expression
import com.andoni.signosdaily.data.SignRef
import com.andoni.signosdaily.data.Word
import com.andoni.signosdaily.data.StudyItem
import com.andoni.signosdaily.data.UserData
import com.andoni.signosdaily.ui.AppViewModel
import kotlinx.coroutines.delay

private enum class LibraryFilter(val label: String) {
    ALL("Todo"), ALPHABET("Abecedario"), WORDS("Signos"), EXPRESSIONS("Expresiones"),
    FAVORITES("Favoritos"), PENDING("Por aprender"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: AppViewModel, user: UserData) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var spellText by rememberSaveable { mutableStateOf("") }
    var openId by remember { mutableStateOf<String?>(null) }
    var openLetter by remember { mutableStateOf<SignRef?>(null) }
    val alphabet = filter == LibraryFilter.ALPHABET

    val seenItems = remember(user.seen) {
        user.seen.mapNotNull(vm.content::byId)
            .sortedWith(compareBy<StudyItem>({ it is Expression }, { it.headword.trimStart('¿', '¡').lowercase() }))
    }
    val shown = seenItems.filter { item ->
        when (filter) {
            LibraryFilter.ALL -> true
            LibraryFilter.ALPHABET -> false
            LibraryFilter.WORDS -> item is Word
            LibraryFilter.EXPRESSIONS -> item is Expression
            LibraryFilter.FAVORITES -> item.id in user.favorites
            LibraryFilter.PENDING -> item.id !in user.learned
        }
    }.filter {
        query.isBlank() || it.headword.contains(query.trim(), ignoreCase = true) ||
            it.gloss.contains(query.trim(), ignoreCase = true) || it.category.contains(query.trim(), ignoreCase = true)
    }
    val totalContent = vm.content.words.size + vm.content.expressions.size

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Biblioteca", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Has descubierto ${seenItems.size} de $totalContent · ${user.learned.size} aprendidos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = if (alphabet) spellText else query,
                onValueChange = { if (alphabet) spellText = it else query = it },
                placeholder = { Text(if (alphabet) "Escribe una palabra para deletrearla" else "Buscar una palabra, frase o tema") },
                leadingIcon = { Icon(if (alphabet) Icons.Outlined.Keyboard else Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                LibraryFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }

        if (alphabet) {
            AlphabetGrid(vm, user, spellText, onOpenLetter = { openLetter = it })
        } else if (shown.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📭", style = MaterialTheme.typography.displaySmall)
                Text(
                    if (seenItems.isEmpty()) "Tu biblioteca se irá llenando día a día."
                    else "Nada por aquí con ese filtro.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(shown, key = { it.id }) { item ->
                    LibraryRow(
                        item = item,
                        favorite = item.id in user.favorites,
                        learned = item.id in user.learned,
                        onClick = { openId = item.id },
                    )
                }
            }
        }
    }

    val letter = openLetter
    if (letter != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { openLetter = null }, sheetState = sheetState) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("LETRA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(letter.label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                SignPlayer(
                    signs = listOf(letter),
                    speed = user.videoSpeed,
                    mirror = user.mirror,
                    onSpeedChange = vm::setVideoSpeed,
                    onMirrorChange = vm::setMirror,
                )
            }
        }
    }

    val open = openId?.let(vm.content::byId)
    if (open != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { openId = null }, sheetState = sheetState) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            ) {
                StudyCard(
                    item = open,
                    isFavorite = open.id in user.favorites,
                    isLearned = open.id in user.learned,
                    speed = user.videoSpeed,
                    mirror = user.mirror,
                    onSpeedChange = vm::setVideoSpeed,
                    onMirrorChange = vm::setMirror,
                    onToggleFavorite = { vm.setFavorite(open.id, open.id !in user.favorites) },
                    onToggleLearned = { vm.setLearned(open.id, open.id !in user.learned) },
                    onShare = { shareItem(context, open) },
                    usedIn = usedInSeen(vm, user, open),
                    onOpenItem = { openId = it.id },
                )
            }
        }
    }
}

@Composable
private fun LibraryRow(item: StudyItem, favorite: Boolean, learned: Boolean, onClick: () -> Unit) {
    val accent = if (item is Word) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = accent) {
                Text(
                    if (item is Word) "S" else "E",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.headword, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (item is Word) item.category else item.gloss,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (favorite) Icon(Icons.Filled.Favorite, "Favorito", tint = Color(0xFFE11D48))
            if (learned) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.CheckCircle, "Aprendido", tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

/** The fingerspelling alphabet: a player for the word being spelled and a grid of letters. */
@Composable
private fun AlphabetGrid(vm: AppViewModel, user: UserData, text: String, onOpenLetter: (SignRef) -> Unit) {
    // Wait until the user stops typing before loading the videos.
    var spelled by remember { mutableStateOf(vm.content.spell(text)) }
    LaunchedEffect(text) {
        delay(600)
        spelled = vm.content.spell(text)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(88.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (spelled.isEmpty()) {
                Text(
                    "Con el alfabeto dactilológico se deletrean los nombres y las palabras que no tienen signo. " +
                        "Toca una letra para verla o escribe arriba una palabra (tu nombre, por ejemplo).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Así se deletrea «${text.trim()}»",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        SignPlayer(
                            signs = spelled,
                            speed = user.videoSpeed,
                            mirror = user.mirror,
                            onSpeedChange = vm::setVideoSpeed,
                            onMirrorChange = vm::setMirror,
                        )
                    }
                }
            }
        }
        items(vm.content.letters, key = { it.key!! }) { letter ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.clickable { onOpenLetter(letter) },
            ) {
                Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SignPhoto(letter.key!!, user.mirror, Modifier.fillMaxWidth())
                    Text(
                        letter.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
