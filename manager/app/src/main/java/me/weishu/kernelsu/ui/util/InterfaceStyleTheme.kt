package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val THEME_SCHEMA = "io.github.fixz.apkesu.interface-style-theme"
private const val THEME_VERSION = 3
private const val MAX_THEME_MOTIFS = 48
private val COLOR_PATTERN = Regex("#[0-9a-fA-F]{8}")

@Immutable
data class InterfaceStyleTheme(
    val engine: String,
    val variant: String?,
    val accent: Long,
    val forceDark: Boolean,
    val lightPalette: InterfaceStylePalette,
    val darkPalette: InterfaceStylePalette,
    val scene: InterfaceStyleScene,
    val chrome: InterfaceStyleChrome,
    val glass: InterfaceStyleGlass,
)

@Immutable
data class InterfaceStylePalette(
    val background: Long,
    val backgroundAlt: Long,
    val surface: Long,
    val surfaceAlt: Long,
    val primary: Long,
    val secondary: Long,
    val outline: Long,
    val highlight: Long,
    val shadow: Long,
    val muted: Long,
    val content: Long,
)

@Immutable
data class InterfaceStyleScene(
    val cycleMillis: Int,
    val primaryCount: Int,
    val secondaryCount: Int,
    val speed: Float,
    val angle: Float,
    val minLengthDp: Float,
    val maxLengthDp: Float,
    val minStrokeDp: Float,
    val maxStrokeDp: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
    val gridDp: Float,
    val clearOnCycle: Boolean,
    val lightning: Boolean,
    val motifs: List<InterfaceStyleMotif>,
)

@Immutable
data class InterfaceStyleChrome(
    val cardAlpha: Float,
    val borderAlpha: Float,
    val cornerDp: Float,
    val unitDp: Float,
    val topBarAlpha: Float,
    val navigationAlpha: Float,
)

@Immutable
data class InterfaceStyleGlass(
    val surfaceAlpha: Float,
    val blurDp: Float,
    val strokeAlpha: Float,
    val refraction: Boolean,
    val refractionHeightDp: Float,
    val refractionAmountDp: Float,
    val chromaticAberration: Float,
)

@Immutable
data class InterfaceStyleMotif(
    val type: InterfaceStyleMotifType,
    val color: InterfaceStyleColorToken,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val alpha: Float,
    val strokeDp: Float,
    val repeatX: Int,
    val repeatY: Int,
    val driftX: Float,
    val driftY: Float,
)

enum class InterfaceStyleMotifType(val wireName: String) {
    Rectangle("rect"),
    Circle("circle"),
    Line("line");

    companion object {
        fun fromWireName(value: String): InterfaceStyleMotifType =
            entries.firstOrNull { it.wireName == value }
                ?: error("Interface style motif type is invalid")
    }
}

enum class InterfaceStyleColorToken(val wireName: String) {
    Primary("primary"),
    Secondary("secondary"),
    Outline("outline"),
    Highlight("highlight"),
    Shadow("shadow"),
    Muted("muted"),
    Content("content");

    companion object {
        fun fromWireName(value: String): InterfaceStyleColorToken =
            entries.firstOrNull { it.wireName == value }
                ?: error("Interface style motif color is invalid")
    }
}

val LocalInterfaceStyleTheme = staticCompositionLocalOf<InterfaceStyleTheme?> { null }

internal fun parseInterfaceStyleTheme(
    bytes: ByteArray,
    expected: InterfaceStylePackage,
): InterfaceStyleTheme {
    val root = JSONObject(bytes.toString(Charsets.UTF_8))
    root.requireExactKeys(
        "schema", "version", "engine", "variant", "accent", "forceDark",
        "palette", "scene", "chrome", "glass",
    )
    require(root.getString("schema") == THEME_SCHEMA) { "Interface style theme schema is invalid" }
    require(root.getInt("version") == THEME_VERSION && expected.version == THEME_VERSION) {
        "Interface style theme version is unsupported"
    }
    require(root.getString("engine") == expected.engine) { "Interface style theme engine does not match catalog" }
    val variant = root.optString("variant").takeIf(String::isNotBlank)
    require(variant == expected.variant) { "Interface style theme variant does not match catalog" }
    require(root.getLong("accent") == expected.accent) { "Interface style theme accent does not match catalog" }

    val palette = root.getJSONObject("palette").also { it.requireExactKeys("light", "dark") }
    return InterfaceStyleTheme(
        engine = expected.engine,
        variant = variant,
        accent = expected.accent,
        forceDark = root.strictBoolean("forceDark"),
        lightPalette = parsePalette(palette.getJSONObject("light")),
        darkPalette = parsePalette(palette.getJSONObject("dark")),
        scene = parseScene(root.getJSONObject("scene")),
        chrome = parseChrome(root.getJSONObject("chrome")),
        glass = parseGlass(root.getJSONObject("glass")),
    )
}

private fun parsePalette(value: JSONObject): InterfaceStylePalette {
    value.requireExactKeys(
        "background", "backgroundAlt", "surface", "surfaceAlt", "primary", "secondary",
        "outline", "highlight", "shadow", "muted", "content",
    )
    return InterfaceStylePalette(
        background = value.strictColor("background"),
        backgroundAlt = value.strictColor("backgroundAlt"),
        surface = value.strictColor("surface"),
        surfaceAlt = value.strictColor("surfaceAlt"),
        primary = value.strictColor("primary"),
        secondary = value.strictColor("secondary"),
        outline = value.strictColor("outline"),
        highlight = value.strictColor("highlight"),
        shadow = value.strictColor("shadow"),
        muted = value.strictColor("muted"),
        content = value.strictColor("content"),
    )
}

private fun parseScene(value: JSONObject): InterfaceStyleScene {
    value.requireExactKeys(
        "cycleMillis", "primaryCount", "secondaryCount", "speed", "angle",
        "minLengthDp", "maxLengthDp", "minStrokeDp", "maxStrokeDp",
        "minAlpha", "maxAlpha", "gridDp", "clearOnCycle", "lightning", "motifs",
    )
    val motifs = parseMotifs(value.getJSONArray("motifs"))
    val minLength = value.strictFloat("minLengthDp", 0f, 96f)
    val maxLength = value.strictFloat("maxLengthDp", 0f, 96f)
    val minStroke = value.strictFloat("minStrokeDp", 0.1f, 8f)
    val maxStroke = value.strictFloat("maxStrokeDp", 0.1f, 8f)
    val minAlpha = value.strictFloat("minAlpha", 0f, 1f)
    val maxAlpha = value.strictFloat("maxAlpha", 0f, 1f)
    require(minLength <= maxLength && minStroke <= maxStroke && minAlpha <= maxAlpha) {
        "Interface style scene ranges are invalid"
    }
    return InterfaceStyleScene(
        cycleMillis = value.strictInt("cycleMillis", 2_000, 60_000),
        primaryCount = value.strictInt("primaryCount", 0, 256),
        secondaryCount = value.strictInt("secondaryCount", 0, 96),
        speed = value.strictFloat("speed", 0f, 4f),
        angle = value.strictFloat("angle", -1f, 1f),
        minLengthDp = minLength,
        maxLengthDp = maxLength,
        minStrokeDp = minStroke,
        maxStrokeDp = maxStroke,
        minAlpha = minAlpha,
        maxAlpha = maxAlpha,
        gridDp = value.strictFloat("gridDp", 4f, 96f),
        clearOnCycle = value.strictBoolean("clearOnCycle"),
        lightning = value.strictBoolean("lightning"),
        motifs = motifs,
    )
}

private fun parseChrome(value: JSONObject): InterfaceStyleChrome {
    value.requireExactKeys(
        "cardAlpha", "borderAlpha", "cornerDp", "unitDp", "topBarAlpha", "navigationAlpha",
    )
    return InterfaceStyleChrome(
        cardAlpha = value.strictFloat("cardAlpha", 0.1f, 1f),
        borderAlpha = value.strictFloat("borderAlpha", 0f, 1f),
        cornerDp = value.strictFloat("cornerDp", 0f, 32f),
        unitDp = value.strictFloat("unitDp", 0.5f, 8f),
        topBarAlpha = value.strictFloat("topBarAlpha", 0f, 1f),
        navigationAlpha = value.strictFloat("navigationAlpha", 0f, 1f),
    )
}

private fun parseGlass(value: JSONObject): InterfaceStyleGlass {
    value.requireExactKeys(
        "surfaceAlpha", "blurDp", "strokeAlpha", "refraction",
        "refractionHeightDp", "refractionAmountDp", "chromaticAberration",
    )
    return InterfaceStyleGlass(
        surfaceAlpha = value.strictFloat("surfaceAlpha", 0.1f, 1f),
        blurDp = value.strictFloat("blurDp", 4f, 48f),
        strokeAlpha = value.strictFloat("strokeAlpha", 0f, 1f),
        refraction = value.strictBoolean("refraction"),
        refractionHeightDp = value.strictFloat("refractionHeightDp", 0f, 48f),
        refractionAmountDp = value.strictFloat("refractionAmountDp", 0f, 32f),
        chromaticAberration = value.strictFloat("chromaticAberration", 0f, 1f),
    )
}

private fun parseMotifs(items: JSONArray): List<InterfaceStyleMotif> {
    require(items.length() <= MAX_THEME_MOTIFS) { "Interface style motif limit exceeded" }
    return buildList(items.length()) {
        for (index in 0 until items.length()) {
            val value = items.optJSONObject(index) ?: error("Interface style motif is invalid")
            value.requireExactKeys(
                "type", "color", "x", "y", "width", "height", "alpha", "strokeDp",
                "repeatX", "repeatY", "driftX", "driftY",
            )
            add(
                InterfaceStyleMotif(
                    type = InterfaceStyleMotifType.fromWireName(value.getString("type")),
                    color = InterfaceStyleColorToken.fromWireName(value.getString("color")),
                    x = value.strictFloat("x", 0f, 1f),
                    y = value.strictFloat("y", 0f, 1f),
                    width = value.strictFloat("width", 0.001f, 1f),
                    height = value.strictFloat("height", 0.001f, 1f),
                    alpha = value.strictFloat("alpha", 0f, 1f),
                    strokeDp = value.strictFloat("strokeDp", 0.1f, 8f),
                    repeatX = value.strictInt("repeatX", 1, 16),
                    repeatY = value.strictInt("repeatY", 1, 16),
                    driftX = value.strictFloat("driftX", -1f, 1f),
                    driftY = value.strictFloat("driftY", -1f, 1f),
                )
            )
        }
    }
}

private fun JSONObject.requireExactKeys(vararg expected: String) {
    val actual = keys().asSequence().toSet()
    require(actual == expected.toSet()) {
        "Interface style theme contains missing or unsupported fields"
    }
}

private fun JSONObject.strictColor(name: String): Long {
    val value = getString(name)
    require(COLOR_PATTERN.matches(value)) { "Interface style color is invalid" }
    return value.substring(1).lowercase(Locale.ROOT).toLong(16)
}

private fun JSONObject.strictFloat(name: String, minimum: Float, maximum: Float): Float {
    val raw = get(name)
    require(raw is Number) { "Interface style number is invalid" }
    val value = raw.toFloat()
    require(value.isFinite() && value in minimum..maximum) { "Interface style number is out of range" }
    return value
}

private fun JSONObject.strictInt(name: String, minimum: Int, maximum: Int): Int {
    val raw = get(name)
    require(raw is Number) { "Interface style integer is invalid" }
    val longValue = raw.toLong()
    require(raw.toDouble() == longValue.toDouble() && longValue in minimum.toLong()..maximum.toLong()) {
        "Interface style integer is out of range"
    }
    return longValue.toInt()
}

private fun JSONObject.strictBoolean(name: String): Boolean {
    val value = get(name)
    require(value is Boolean) { "Interface style boolean is invalid" }
    return value
}
