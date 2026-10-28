package com.andoni.signosdaily.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andoni.signosdaily.BuildConfig
import com.andoni.signosdaily.DemoMode
import com.andoni.signosdaily.data.ContentMode
import com.andoni.signosdaily.data.ContentRepository
import com.andoni.signosdaily.data.Expression
import com.andoni.signosdaily.data.StudyItem
import com.andoni.signosdaily.data.ThemeMode
import com.andoni.signosdaily.data.UserData
import com.andoni.signosdaily.data.UserRepository
import com.andoni.signosdaily.data.Word
import com.andoni.signosdaily.domain.Achievements
import com.andoni.signosdaily.domain.StatsCalculator
import com.andoni.signosdaily.media.MediaStore
import com.andoni.signosdaily.notify.ReminderReceiver
import com.andoni.signosdaily.notify.ReminderScheduler
import com.andoni.signosdaily.update.UpdateInfo
import com.andoni.signosdaily.update.Updater
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class QuizKind {
    /** Watch the sign, pick its meaning. */
    SIGN_TO_MEANING,
    /** Read the word, pick its sign among four photos. */
    MEANING_TO_SIGN,
    /** Read the sentence, pick the signs in LSE order (works offline). */
    GLOSS_ORDER,
}

data class QuizQuestion(
    val item: StudyItem,
    val kind: QuizKind,
    val prompt: String,
    val hint: String,
    val options: List<String>,
    /** For [QuizKind.MEANING_TO_SIGN]: the media key shown for each option. */
    val optionKeys: List<String> = emptyList(),
    val correct: Int,
)

/** Progress of "download every sign" in Settings. */
data class DownloadState(val done: Int, val total: Int, val finished: Boolean = false, val failed: Int = 0)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val content = ContentRepository.get(app)
    private val users = UserRepository.get(app)
    val media = MediaStore.get(app)

    val user: StateFlow<UserData?> = users.data.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _newAchievements = MutableStateFlow<List<String>>(emptyList())
    val newAchievements: StateFlow<List<String>> = _newAchievements.asStateFlow()

    /** Newer release found on GitHub, shown as a dialog. */
    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update.asStateFlow()

    /** Download progress 0..1 while an update downloads, null otherwise. */
    private val _updateProgress = MutableStateFlow<Float?>(null)
    val updateProgress: StateFlow<Float?> = _updateProgress.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    private val _download = MutableStateFlow<DownloadState?>(null)
    val download: StateFlow<DownloadState?> = _download.asStateFlow()

    private var lastUpdateCheck = 0L
    private var prefetchJob: Job? = null

    fun checkForUpdates(manual: Boolean = false) = viewModelScope.launch {
        if (!BuildConfig.SELF_UPDATE || DemoMode.active) return@launch
        val now = System.currentTimeMillis()
        if (!manual && now - lastUpdateCheck < 60 * 60 * 1000L) return@launch
        lastUpdateCheck = now
        if (manual) _updateMessage.value = "Buscando…"
        val found = Updater.check()
        if (found != null) {
            _update.value = found
            _updateMessage.value = null
        } else if (manual) {
            _updateMessage.value = "Ya tienes la última versión."
        }
    }

    fun startUpdate() = viewModelScope.launch {
        val info = _update.value ?: return@launch
        _updateProgress.value = 0f
        runCatching {
            val apk = Updater.download(getApplication(), info) { _updateProgress.value = it }
            Updater.install(getApplication(), apk)
        }.onFailure {
            _updateMessage.value = "No se pudo descargar la actualización. Revisa tu conexión."
        }
        _updateProgress.value = null
        _update.value = null
    }

    fun dismissUpdate() {
        _update.value = null
    }

    fun todayItems(user: UserData, today: LocalDate = LocalDate.now()): List<StudyItem> = itemsForDay(user, today)

    private fun itemsForDay(user: UserData, date: LocalDate): List<StudyItem> {
        val day = user.dayIndex(date)
        val word = content.wordForDay(day)
        val expression = content.expressionForDay(day)
        val swapped = date in user.swappedDays
        return when (user.contentMode) {
            ContentMode.BOTH -> listOf(word, expression)
            ContentMode.WORD -> listOf(if (swapped) expression else word)
            ContentMode.EXPRESSION -> listOf(if (swapped) word else expression)
        }
    }

    /** Called every time the app comes to the foreground. */
    fun onAppOpened() = viewModelScope.launch {
        checkForUpdates()
        val u = users.current()
        if (!u.onboarded) return@launch
        users.recordVisit()
        users.markSeen(todayItems(users.current()).map { it.id })
        checkAchievements()
        if (u.notificationsEnabled) ReminderScheduler.schedule(getApplication(), u.reminderHour, u.reminderMinute)
        prefetch()
    }

    /** Keeps today's and tomorrow's signs, and everything already seen, ready for offline use. */
    private fun prefetch() {
        if (prefetchJob?.isActive == true || DemoMode.active) return
        prefetchJob = viewModelScope.launch {
            media.refreshCatalog()
            val u = users.current()
            val today = LocalDate.now()
            val upcoming = (0L..1L).flatMap { itemsForDay(u.copy(swappedDays = emptySet()), today.plusDays(it)) }
            val keys = (upcoming + u.seen.mapNotNull(content::byId)).flatMap { it.mediaKeys }.distinct()
            media.prefetch(keys)
        }
    }

    fun completeOnboarding(
        name: String,
        mode: ContentMode,
        mirror: Boolean,
        hour: Int,
        minute: Int,
        notifications: Boolean,
    ) = viewModelScope.launch {
        users.completeOnboarding(name, mode, mirror, hour, minute, notifications)
        if (notifications) ReminderScheduler.schedule(getApplication(), hour, minute)
        onAppOpened()
    }

    private suspend fun checkAchievements() {
        val u = users.current()
        val stats = StatsCalculator.compute(u.visits)
        val earned = Achievements.newlyEarned(u, stats)
        if (earned.isNotEmpty()) {
            users.unlockAchievements(earned)
            _newAchievements.update { it + earned }
        }
    }

    fun dismissAchievements() = _newAchievements.update { emptyList() }

    fun setLearned(id: String, learned: Boolean) = viewModelScope.launch {
        users.setLearned(id, learned)
        checkAchievements()
    }

    fun setFavorite(id: String, favorite: Boolean) = viewModelScope.launch {
        users.setFavorite(id, favorite)
        checkAchievements()
    }

    fun swapToday() = viewModelScope.launch {
        users.toggleSwap()
        users.markSeen(todayItems(users.current()).map { it.id })
    }

    fun setName(name: String) = viewModelScope.launch { users.setName(name) }
    fun setContentMode(mode: ContentMode) = viewModelScope.launch {
        users.setContentMode(mode)
        users.markSeen(todayItems(users.current()).map { it.id })
    }
    fun setTheme(theme: ThemeMode) = viewModelScope.launch { users.setTheme(theme) }
    fun setVideoSpeed(speed: Float) = viewModelScope.launch { users.setVideoSpeed(speed) }
    fun setMirror(mirror: Boolean) = viewModelScope.launch { users.setMirror(mirror) }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        users.setNotificationsEnabled(enabled)
        val u = users.current()
        if (enabled) ReminderScheduler.schedule(getApplication(), u.reminderHour, u.reminderMinute)
        else ReminderScheduler.cancel(getApplication())
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        users.setReminderTime(hour, minute)
        if (users.current().notificationsEnabled) ReminderScheduler.schedule(getApplication(), hour, minute)
    }

    fun sendTestNotification() = viewModelScope.launch {
        ReminderReceiver.remind(getApplication(), force = true)
    }

    fun resetProgress() = viewModelScope.launch {
        users.resetProgress()
        onAppOpened()
    }

    // ---- Offline videos ----

    fun downloadAllSigns() {
        if (_download.value?.finished == false) return
        viewModelScope.launch {
            media.refreshCatalog(force = true)
            _download.value = DownloadState(0, 0)
            val failed = media.downloadAll(content.allMediaKeys) { done, total -> _download.value = DownloadState(done, total) }
            _download.update { (it ?: DownloadState(0, 0)).copy(finished = true, failed = failed) }
        }
    }

    fun clearDownloads() {
        media.clear()
        _download.value = null
    }

    // ---- Quiz ----

    fun buildQuiz(user: UserData, size: Int = 6): List<QuizQuestion> {
        val pool = (user.seen.mapNotNull(content::byId) + todayItems(user)).distinctBy { it.id }
        if (pool.isEmpty()) return emptyList()
        val online = media.isOnline()
        fun canShow(key: String) = media.hasVideo(key) || (online && !media.isMissing(key))
        // Prefer items not yet marked as learned, then mix in the rest.
        val ordered = pool.shuffled().sortedBy { it.id in user.learned }
        val questions = mutableListOf<QuizQuestion>()
        var round = 0
        while (questions.size < size && round < 2) {
            for (item in ordered) {
                if (questions.size >= size) break
                val alt = (round + questions.size) % 2 == 1
                makeQuestion(item, alt, ::canShow)?.let { questions += it }
            }
            round++
        }
        return questions.shuffled()
    }

    private fun makeQuestion(item: StudyItem, alt: Boolean, canShow: (String) -> Boolean): QuizQuestion? {
        val videoOk = item.mediaKeys.isNotEmpty() && item.mediaKeys.all(canShow)
        return when (item) {
            is Word -> {
                if (!videoOk) return null
                if (alt) wordToSign(item, canShow) ?: signToMeaning(item, content.words)
                else signToMeaning(item, content.words)
            }
            is Expression -> {
                if (videoOk && !alt) signToMeaning(item, content.expressions) else glossOrder(item)
            }
        }
    }

    private fun signToMeaning(item: StudyItem, all: List<StudyItem>): QuizQuestion {
        val distractors = all.filter { it.id != item.id }.shuffled().map { it.headword }
            .filter { it != item.headword }.distinct().take(3)
        val options = (distractors + item.headword).shuffled()
        val isWord = item is Word
        return QuizQuestion(
            item = item,
            kind = QuizKind.SIGN_TO_MEANING,
            prompt = if (isWord) "¿Qué significa este signo?" else "¿Qué dice esta frase?",
            hint = if (isWord) "Mira el vídeo y elige la palabra." else "Mira los signos y elige la frase.",
            options = options,
            correct = options.indexOf(item.headword),
        )
    }

    private fun wordToSign(item: Word, canShow: (String) -> Boolean): QuizQuestion? {
        val key = item.sign.key ?: return null
        val others = content.words.filter { it.index != item.index && it.sign.key != key }
            .filter { w -> w.sign.key?.let(canShow) == true }
            .shuffled().take(3)
        if (others.size < 3) return null
        val options = (others + item).shuffled()
        return QuizQuestion(
            item = item,
            kind = QuizKind.MEANING_TO_SIGN,
            prompt = "¿Cuál es el signo de «${item.headword}»?",
            hint = "Toca la foto correcta.",
            options = options.map { it.headword },
            optionKeys = options.map { it.sign.key!! },
            correct = options.indexOf(item),
        )
    }

    /** Asks for the LSE order: distractors are the same signs shuffled, or other sentences' glosses. */
    private fun glossOrder(item: Expression): QuizQuestion {
        val labels = item.signs.map { it.label }
        val correct = item.gloss
        val shuffles = if (labels.size >= 3) {
            generateSequence { labels.shuffled().joinToString(" ") }.take(40).filter { it != correct }.distinct().take(3).toList()
        } else emptyList()
        val distractors = (shuffles + content.expressions.filter { it.index != item.index }.shuffled().map { it.gloss })
            .filter { it != correct }.distinct().take(3)
        val options = (distractors + correct).shuffled()
        return QuizQuestion(
            item = item,
            kind = QuizKind.GLOSS_ORDER,
            prompt = "¿Cómo se signa «${item.headword}»?",
            hint = "Elige los signos en el orden de la LSE.",
            options = options,
            correct = options.indexOf(correct),
        )
    }

    fun finishQuiz(correct: Int, total: Int) = viewModelScope.launch {
        users.recordQuiz(correct, total)
        checkAchievements()
    }
}
