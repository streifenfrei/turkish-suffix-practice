package com.example.suffixtrainer.data

import com.example.suffixtrainer.model.Category
import com.example.suffixtrainer.model.Sentence
import com.example.suffixtrainer.model.Suffix
import com.example.suffixtrainer.model.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory [CardRepository] backed by a small hand-built corpus. Uses the real :core-model
 * entities so a Room-backed implementation drops in untouched. All [Sentence.audioPath]s are
 * null — there is no real audio yet, and the player no-ops gracefully on null.
 */
@Singleton
class FakeCardRepository @Inject constructor() : CardRepository {
    override fun observeCorpus(): Flow<List<CardData>> = flowOf(SAMPLE_CORPUS)
}

/**
 * Hand-built sample sentences covering most categories. Each suffix's `startInSurface`/
 * `endInSurface` is the verified span within its token's surface form; token char spans within
 * the sentence are derived by [buildSentence]. Asserted by BlankingTest.
 */
val SAMPLE_CORPUS: List<CardData> = listOf(
    // "Evde kaldım." — ev+LOCATIVE(-de), kal+PAST_DEFINITE(-dı)
    buildSentence(1, 100, "Evde kaldım.", "I stayed at home") {
        token("Evde", "ev") { suffix("de", Category.LOCATIVE, 2, 4) }
        token("kaldım", "kal") { suffix("dı", Category.PAST_DEFINITE, 3, 5) }
    },
    // "Okula gidiyorum." — okul+DATIVE(-a), git+PRESENT_CONTINUOUS(-iyor)
    buildSentence(2, 101, "Okula gidiyorum.", "I am going to school") {
        token("Okula", "okul") { suffix("a", Category.DATIVE, 4, 5) }
        token("gidiyorum", "git") { suffix("iyor", Category.PRESENT_CONTINUOUS, 3, 7) }
    },
    // "Kitabı okudum." — kitap+ACCUSATIVE(-ı), oku+PAST_DEFINITE(-du)
    buildSentence(3, 102, "Kitabı okudum.", "I read the book") {
        token("Kitabı", "kitap") { suffix("ı", Category.ACCUSATIVE, 5, 6) }
        token("okudum", "oku") { suffix("du", Category.PAST_DEFINITE, 3, 5) }
    },
    // "Evden geldim." — ev+ABLATIVE(-den), gel+PAST_DEFINITE(-di)
    buildSentence(4, 103, "Evden geldim.", "I came from home") {
        token("Evden", "ev") { suffix("den", Category.ABLATIVE, 2, 5) }
        token("geldim", "gel") { suffix("di", Category.PAST_DEFINITE, 3, 5) }
    },
    // "Yarın geleceğim." — gel+FUTURE(-eceğ). "Yarın" (tomorrow) carries no target suffix.
    buildSentence(5, 104, "Yarın geleceğim.", "I will come tomorrow") {
        token("Yarın", "yarın") {}
        token("geleceğim", "gel") { suffix("eceğ", Category.FUTURE, 3, 7) }
    },
    // "Bıçakla kestim." — bıçak+INSTRUMENTAL(-la), kes+PAST_DEFINITE(-ti)
    buildSentence(6, 105, "Bıçakla kestim.", "I cut it with a knife") {
        token("Bıçakla", "bıçak") { suffix("la", Category.INSTRUMENTAL, 5, 7) }
        token("kestim", "kes") { suffix("ti", Category.PAST_DEFINITE, 3, 5) }
    },
    // "Annenin evi." — anne+GENITIVE(-nin). "evi" possessive has no category in our enum.
    buildSentence(7, 106, "Annenin evi.", "Mother's house") {
        token("Annenin", "anne") { suffix("nin", Category.GENITIVE, 4, 7) }
        token("evi", "ev") {}
    },
    // "Çay içerim." — iç+AORIST(-er). "Çay" carries no target suffix.
    buildSentence(8, 107, "Çay içerim.", "I drink tea") {
        token("Çay", "çay") {}
        token("içerim", "iç") { suffix("er", Category.AORIST, 2, 4) }
    },
    // "O gelmiş." — gel+PAST_INFERENTIAL(-miş). "O" (he/she) carries no target suffix.
    buildSentence(9, 108, "O gelmiş.", "Apparently he came") {
        token("O", "o") {}
        token("gelmiş", "gel") { suffix("miş", Category.PAST_INFERENTIAL, 3, 6) }
    },
    // "Okula gitmeliyim." — okul+DATIVE(-a), git+NECESSITATIVE(-meli)
    buildSentence(10, 109, "Okula gitmeliyim.", "I must go to school") {
        token("Okula", "okul") { suffix("a", Category.DATIVE, 4, 5) }
        token("gitmeliyim", "git") { suffix("meli", Category.NECESSITATIVE, 3, 7) }
    },
    // "Eve gitsem." — ev+DATIVE(-e), git+CONDITIONAL(-se)
    buildSentence(11, 110, "Eve gitsem.", "If I go home") {
        token("Eve", "ev") { suffix("e", Category.DATIVE, 2, 3) }
        token("gitsem", "git") { suffix("se", Category.CONDITIONAL, 3, 5) }
    },
    // "Eve gidelim." — ev+DATIVE(-e), git+OPTATIVE(-e)
    buildSentence(12, 111, "Eve gidelim.", "Let's go home") {
        token("Eve", "ev") { suffix("e", Category.DATIVE, 2, 3) }
        token("gidelim", "git") { suffix("e", Category.OPTATIVE, 3, 4) }
    },
)

// --- builder ---------------------------------------------------------------------------------

@DslMarker
private annotation class CorpusDsl

@CorpusDsl
private class SentenceScope(private val turkishText: String) {
    val tokens = mutableListOf<TokenSpec>()
    private var cursor = 0

    fun token(surface: String, lemma: String, block: TokenScope.() -> Unit) {
        val start = turkishText.indexOf(surface, cursor)
        require(start >= 0) { "Token '$surface' not found in \"$turkishText\" after index $cursor" }
        cursor = start + surface.length
        val scope = TokenScope(surface).apply(block)
        tokens += TokenSpec(surface, lemma, start, cursor, scope.suffixes)
    }
}

@CorpusDsl
private class TokenScope(private val surface: String) {
    val suffixes = mutableListOf<SuffixSpec>()

    fun suffix(morpheme: String, category: Category, startInSurface: Int, endInSurface: Int) {
        require(surface.substring(startInSurface, endInSurface) == morpheme) {
            "Suffix span [$startInSurface,$endInSurface) of '$surface' is " +
                "'${surface.substring(startInSurface, endInSurface)}', expected '$morpheme'"
        }
        suffixes += SuffixSpec(morpheme, category, startInSurface, endInSurface)
    }
}

private class TokenSpec(
    val surface: String,
    val lemma: String,
    val charStart: Int,
    val charEnd: Int,
    val suffixes: List<SuffixSpec>,
)

private class SuffixSpec(
    val morpheme: String,
    val category: Category,
    val startInSurface: Int,
    val endInSurface: Int,
)

private fun buildSentence(
    id: Long,
    tatoebaId: Long,
    turkish: String,
    english: String,
    block: SentenceScope.() -> Unit,
): CardData {
    val scope = SentenceScope(turkish).apply(block)
    val sentence = Sentence(
        id = id,
        turkishText = turkish,
        englishText = english,
        audioPath = null,
        tatoebaId = tatoebaId,
    )
    val tokens = scope.tokens.mapIndexed { tIndex, spec ->
        val tokenId = id * 100 + tIndex
        val token = Token(
            id = tokenId,
            sentenceId = id,
            position = tIndex,
            surface = spec.surface,
            lemma = spec.lemma,
            charStart = spec.charStart,
            charEnd = spec.charEnd,
        )
        val suffixes = spec.suffixes.mapIndexed { sIndex, s ->
            Suffix(
                id = tokenId * 10 + sIndex,
                tokenId = tokenId,
                morpheme = s.morpheme,
                category = s.category,
                startInSurface = s.startInSurface,
                endInSurface = s.endInSurface,
            )
        }
        TokenWithSuffixes(token, suffixes)
    }
    return CardData(sentence, tokens)
}
