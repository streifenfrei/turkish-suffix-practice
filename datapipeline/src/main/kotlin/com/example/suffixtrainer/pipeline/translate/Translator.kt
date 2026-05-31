package com.example.suffixtrainer.pipeline.translate

import com.example.suffixtrainer.pipeline.PipelineConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * Turkish→English machine translation, used at build time only to pre-generate word glosses.
 * Implementations call a paid translation API; the key is read from the environment and never
 * logged or persisted. Batches are aligned: the result list matches the input order.
 */
interface Translator {
    val name: String
    fun translate(texts: List<String>): List<String>

    companion object {
        /** Max texts per request both providers accept comfortably. */
        const val BATCH = 50

        fun fromConfig(config: PipelineConfig): Translator = when (config.translator) {
            "deepl" -> DeepLTranslator(requireKey("DEEPL_AUTH_KEY", "deepl"))
            "google" -> GoogleV2Translator(requireKey("GOOGLE_TRANSLATE_API_KEY", "google"))
            else -> error("Unknown --translator '${config.translator}' (expected deepl|google).")
        }

        /**
         * The API key from the env var [env], or a gitignored `local.<provider>.key` file (checked
         * in the working dir and the repo root). Never logged.
         */
        private fun requireKey(env: String, provider: String): String {
            System.getenv(env)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
            for (f in listOf(java.io.File("local.$provider.key"), java.io.File("../local.$provider.key"))) {
                if (f.isFile) f.readText().trim().takeIf { it.isNotBlank() }?.let { return it }
            }
            error("Set $env or create a gitignored local.$provider.key file (or pass --no-glosses).")
        }
    }
}

private val httpClient: HttpClient = HttpClient.newHttpClient()

private fun postForm(url: String, headers: Map<String, String>, body: String): String {
    var builder = HttpRequest.newBuilder(URI.create(url))
        .POST(HttpRequest.BodyPublishers.ofString(body))
    headers.forEach { (k, v) -> builder = builder.header(k, v) }
    val resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    check(resp.statusCode() in 200..299) { "translate HTTP ${resp.statusCode()}: ${resp.body().take(300)}" }
    return resp.body()
}

private fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)

/** DeepL API. Free keys (suffix `:fx`) use the api-free host; others use the pro host. */
class DeepLTranslator(private val key: String) : Translator {
    override val name = "DeepL"
    private val host = if (key.endsWith(":fx")) "https://api-free.deepl.com" else "https://api.deepl.com"

    override fun translate(texts: List<String>): List<String> {
        val body = buildString {
            texts.forEach { append("text=").append(enc(it)).append('&') }
            append("source_lang=TR&target_lang=EN-US")
        }
        val json = postForm(
            "$host/v2/translate",
            mapOf(
                "Authorization" to "DeepL-Auth-Key $key",
                "Content-Type" to "application/x-www-form-urlencoded",
            ),
            body,
        )
        val arr = JSONObject(json).getJSONArray("translations")
        return List(arr.length()) { arr.getJSONObject(it).getString("text") }
    }
}

/** Google Cloud Translation API v2 (key auth). */
class GoogleV2Translator(private val key: String) : Translator {
    override val name = "Google v2"

    override fun translate(texts: List<String>): List<String> {
        val payload = JSONObject()
            .put("q", JSONArray(texts))
            .put("source", "tr")
            .put("target", "en")
            .put("format", "text")
        val json = postForm(
            "https://translation.googleapis.com/language/translate/v2?key=${enc(key)}",
            mapOf("Content-Type" to "application/json; charset=utf-8"),
            payload.toString(),
        )
        val arr = JSONObject(json).getJSONObject("data").getJSONArray("translations")
        return List(arr.length()) { arr.getJSONObject(it).getString("translatedText") }
    }
}
