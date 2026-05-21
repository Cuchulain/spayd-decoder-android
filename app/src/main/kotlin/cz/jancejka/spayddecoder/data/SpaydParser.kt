package cz.jancejka.spayddecoder.data

import java.net.URLDecoder
import java.text.NumberFormat
import java.util.Locale

data class SpaydField(
    val key: String,
    val label: String,
    val displayValue: String,
    val copyValue: String,
)

data class ParsedSpayd(
    val version: String,
    val raw: String,
    val fields: List<SpaydField>,
)

object SpaydParser {

    private val LABELS = mapOf(
        "ACC" to "IBAN",
        "AM" to "Částka",
        "CC" to "Měna",
        "RF" to "Reference plátce",
        "RN" to "Jméno příjemce",
        "DT" to "Datum splatnosti",
        "PT" to "Typ platby",
        "MSG" to "Zpráva pro příjemce",
        "X-VS" to "Variabilní symbol",
        "X-SS" to "Specifický symbol",
        "X-KS" to "Konstantní symbol",
        "X-PER" to "Opakování (dny)",
        "X-ID" to "ID platby",
        "X-URL" to "URL",
        "NT" to "Notifikace",
        "NTA" to "Adresa notifikace",
        "ALT-ACC" to "Alternativní účty",
    )

    private val ORDER = listOf(
        "ACC_DERIVED", "ACC", "ACC_BIC", "AM", "CC",
        "X-VS", "X-SS", "X-KS",
        "MSG", "RN", "DT", "PT", "RF",
        "ALT-ACC", "X-PER", "X-ID", "X-URL", "NT", "NTA",
    )

    class SpaydException(message: String) : Exception(message)

    fun parse(text: String): ParsedSpayd {
        val trimmed = text.trim()
        if (!trimmed.startsWith("SPD*", ignoreCase = true)) {
            throw SpaydException("QR kód nevypadá jako SPAYD (chybí prefix SPD*).")
        }
        val parts = trimmed.split("*")
        val version = parts.getOrNull(1).orEmpty()

        val raw = linkedMapOf<String, String>()
        for (i in 2 until parts.size) {
            val token = parts[i]
            if (token.isEmpty()) continue
            val colon = token.indexOf(':')
            if (colon < 0) continue
            val key = token.substring(0, colon).uppercase()
            val value = decodeValue(token.substring(colon + 1))
            raw[key] = value
        }

        val cards = mutableListOf<SpaydField>()

        raw["ACC"]?.let { acc ->
            val split = acc.split("+", limit = 2)
            val iban = split[0]
            val bic = split.getOrNull(1)
            ibanToCzechAccount(iban)?.let {
                cards += SpaydField("ACC_DERIVED", "Číslo účtu (CZ)", it, it)
            }
            cards += SpaydField("ACC", LABELS["ACC"]!!, iban, iban)
            if (!bic.isNullOrBlank()) {
                cards += SpaydField("ACC_BIC", "BIC / SWIFT", bic, bic)
            }
        }

        raw["AM"]?.let { am ->
            val display = formatAmount(am, raw["CC"])
            cards += SpaydField("AM", LABELS["AM"]!!, display, am)
        }
        raw["CC"]?.let { cards += SpaydField("CC", LABELS["CC"]!!, it, it) }

        raw["DT"]?.let {
            val display = formatDate(it)
            cards += SpaydField("DT", LABELS["DT"]!!, display, display)
        }

        val handled = setOf("ACC", "AM", "CC", "DT")
        for ((key, value) in raw) {
            if (key in handled) continue
            val label = LABELS[key] ?: key
            cards += SpaydField(key, label, value, value)
        }

        cards.sortBy { f ->
            val idx = ORDER.indexOf(f.key)
            if (idx == -1) ORDER.size + 1 else idx
        }

        return ParsedSpayd(version = version, raw = trimmed, fields = cards)
    }

    private fun decodeValue(raw: String): String =
        try {
            URLDecoder.decode(raw.replace("+", "%20"), "UTF-8")
        } catch (_: Exception) {
            raw
        }

    /** CZ IBAN: CZkk BBBB PPPP PPCC CCCC CCCC → `prefix-account/bankcode` */
    fun ibanToCzechAccount(iban: String): String? {
        val clean = iban.replace("\\s".toRegex(), "").uppercase()
        if (!clean.matches(Regex("^CZ\\d{22}$"))) return null
        val bank = clean.substring(4, 8)
        val prefixRaw = clean.substring(8, 14)
        val accountRaw = clean.substring(14, 24)
        val prefix = prefixRaw.trimStart('0')
        val account = accountRaw.trimStart('0').ifEmpty { "0" }
        return buildString {
            if (prefix.isNotEmpty()) append(prefix).append('-')
            append(account).append('/').append(bank)
        }
    }

    private fun formatDate(dt: String): String =
        if (dt.matches(Regex("^\\d{8}$")))
            "${dt.substring(6, 8)}.${dt.substring(4, 6)}.${dt.substring(0, 4)}"
        else dt

    private fun formatAmount(am: String, cc: String?): String {
        val num = am.toDoubleOrNull() ?: return am
        val nf = NumberFormat.getNumberInstance(Locale("cs", "CZ")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formatted = nf.format(num)
        return if (!cc.isNullOrBlank()) "$formatted $cc" else formatted
    }
}
