package com.andoni.signosdaily.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class ItemType { WORD, EXPRESSION }

/**
 * One sign in a sequence. [key] names its video and photo in the media release
 * (`<key>.mp4`, `<key>.jpg`); null for parts with no video, like spelling your own name.
 */
data class SignRef(val key: String?, val label: String, val searchTerm: String = label.lowercase())

/** A learnable card: either a single sign (word) or a short sentence (expression). */
sealed interface StudyItem {
    val id: String
    val type: ItemType
    /** The Spanish word or sentence. */
    val headword: String
    val category: String
    /** Signs to play, in LSE order. */
    val signs: List<SignRef>
    /** Gloss: the signs written in capitals, in LSE order. */
    val gloss: String get() = signs.joinToString(" ") { it.label }
    /** Memory aid or grammar note; may be empty. */
    val tip: String
    val mediaKeys: List<String> get() = signs.mapNotNull { it.key }
}

data class Word(
    val index: Int,
    override val headword: String,
    override val category: String,
    val sign: SignRef,
    override val tip: String,
) : StudyItem {
    override val id get() = "w:$index"
    override val type get() = ItemType.WORD
    override val signs get() = listOf(sign)
}

data class Expression(
    val index: Int,
    override val headword: String,
    override val category: String,
    override val signs: List<SignRef>,
    override val tip: String,
) : StudyItem {
    override val id get() = "e:$index"
    override val type get() = ItemType.EXPRESSION
}

/** Bundled course, loaded once from assets/content.json. */
class ContentRepository(context: Context) {

    val words: List<Word>
    val expressions: List<Expression>
    private val signsByKey: Map<String, SignRef>

    init {
        val json = context.applicationContext.assets.open("content.json").bufferedReader().use { JSONObject(it.readText()) }
        val signs = mutableMapOf<String, SignRef>()

        val wordsJson = json.getJSONArray("words")
        words = List(wordsJson.length()) { i ->
            val o = wordsJson.getJSONObject(i)
            val ref = signRef(o.getString("key"), o)
            signs[ref.key!!] = ref
            Word(
                index = i,
                headword = o.getString("w"),
                category = o.getString("cat"),
                sign = ref,
                tip = o.optString("tip"),
            )
        }

        val extra = json.getJSONObject("signs")
        extra.keys().forEach { key -> signs.getOrPut(key) { signRef(key, extra.getJSONObject(key)) } }
        signsByKey = signs

        val exJson = json.getJSONArray("expressions")
        expressions = List(exJson.length()) { i ->
            val o = exJson.getJSONObject(i)
            val tokens = o.getJSONArray("signs")
            Expression(
                index = i,
                headword = o.getString("es"),
                category = o.getString("cat"),
                signs = List(tokens.length()) { t ->
                    val token = tokens.getString(t)
                    // "#..." marks a part without video, e.g. "#A-N-A (tu nombre deletreado)".
                    if (token.startsWith("#")) SignRef(null, token.removePrefix("#"))
                    else signs[token] ?: error("Unknown sign '$token' in expression $i")
                },
                tip = o.optString("note"),
            )
        }
    }

    private fun signRef(key: String, o: JSONObject): SignRef {
        val queries: JSONArray? = o.optJSONArray("q")
        val label = o.optString("label").ifBlank { o.getString("w").uppercase() }
        val term = queries?.optString(0)?.takeIf { it.isNotBlank() } ?: o.optString("w").ifBlank { label.lowercase() }
        return SignRef(key, label, term)
    }

    val allMediaKeys: Set<String> get() = signsByKey.keys

    fun wordForDay(dayIndex: Long): Word = words[Math.floorMod(dayIndex, words.size.toLong()).toInt()]

    fun expressionForDay(dayIndex: Long): Expression =
        expressions[Math.floorMod(dayIndex, expressions.size.toLong()).toInt()]

    /** Expressions that use the given sign, to show a word in context. */
    fun expressionsUsing(key: String): List<Expression> = expressions.filter { e -> e.signs.any { it.key == key } }

    fun byId(id: String): StudyItem? {
        val (kind, num) = id.split(":").let { it.getOrNull(0) to it.getOrNull(1)?.toIntOrNull() }
        num ?: return null
        return when (kind) {
            "w" -> words.getOrNull(num)
            "e" -> expressions.getOrNull(num)
            else -> null
        }
    }

    companion object {
        @Volatile private var instance: ContentRepository? = null
        fun get(context: Context): ContentRepository =
            instance ?: synchronized(this) {
                instance ?: ContentRepository(context).also { instance = it }
            }
    }
}
