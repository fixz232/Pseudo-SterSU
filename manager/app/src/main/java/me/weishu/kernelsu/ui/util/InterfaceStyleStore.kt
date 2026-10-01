package me.weishu.kernelsu.ui.util

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AtomicFile
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ksuApp
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipInputStream

/**
 * A downloaded interface style is data only. It selects one of the renderers
 * already shipped in the Manager; it cannot add code, scripts, dex or native
 * libraries to the application.
 */
data class InterfaceStylePackage(
    val id: String,
    val name: String,
    val summary: String,
    val engine: String,
    val variant: String?,
    val version: Int,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val accent: Long,
)

data class InstalledInterfaceStyle(
    val style: InterfaceStylePackage,
    val installedAt: Long,
)

data class InterfaceStyleCatalog(
    val generatedAt: Long,
    val styles: List<InterfaceStylePackage>,
)

data class InterfaceStyleCatalogSnapshot(
    val catalog: InterfaceStyleCatalog,
    val source: InterfaceStyleCatalogSource,
    val offline: Boolean,
    val errorMessage: String? = null,
)

enum class InterfaceStyleCatalogSource { Network, Cache, Bundled }

enum class InterfaceStyleProxyMode { Direct, Auto, Custom }

data class InterfaceStyleDownloadPreferences(
    val mode: InterfaceStyleProxyMode = InterfaceStyleProxyMode.Auto,
    val customProxy: String = "",
)

data class InterfaceStyleDownloadProgress(
    val downloaded: Long,
    val total: Long,
) {
    val fraction: Float?
        get() = total.takeIf { it > 0L }
            ?.let { (downloaded.toDouble() / it).toFloat().coerceIn(0f, 1f) }
}

private const val INTERFACE_STYLE_SCHEMA = "io.github.fixz.apkesu.interface-style-catalog"
private const val INTERFACE_STYLE_STATE_SCHEMA = "io.github.fixz.apkesu.interface-style-state"
private const val INTERFACE_STYLE_BUNDLE_SCHEMA = "io.github.fixz.apkesu.interface-style-bundle"
private const val INTERFACE_STYLE_VERSION = 3
private const val MAX_CATALOG_BYTES = 512L * 1024L
private const val MAX_SIGNATURE_BYTES = 4L * 1024L
private const val MAX_PACKAGE_BYTES = 8L * 1024L * 1024L
private const val MAX_BUNDLE_ENTRY_BYTES = 6L * 1024L * 1024L
private const val MAX_BUNDLE_UNCOMPRESSED_BYTES = 12L * 1024L * 1024L
private const val MAX_IMAGE_DIMENSION = 4096
private const val MAX_IMAGE_PIXELS = 16_777_216L
private const val CATALOG_ASSET = "interface-style/catalog-v1.json"
private const val CATALOG_SIGNATURE_ASSET = "interface-style/catalog-v1.sig"
private const val CATALOG_CACHE_NAME = "catalog-v1.json"
private const val CATALOG_SIGNATURE_CACHE_NAME = "catalog-v1.sig"
private const val STATE_NAME = "installed-v2.json"
private const val BUNDLES_DIRECTORY_NAME = "packages"
private const val PREFS_NAME = "interface-style-download"
private const val MODE_KEY = "proxy_mode"
private const val CUSTOM_PROXY_KEY = "custom_proxy"
private const val DEFAULT_CATALOG_URL =
    "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/interface-styles/catalog-v1.json"
private const val DEFAULT_CATALOG_SIGNATURE_URL =
    "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/interface-styles/catalog-v1.sig"
private const val CATALOG_PUBLIC_KEY_B64 =
    "MCowBQYDK2VwAyEAwGidBgSY/SZ25RAsBN3O2SpnFX0RuoMpE6wZqy/LaR0="
private const val BUNDLE_MANIFEST_PATH = "manifest.json"
private const val THEME_RESOURCE_PATH = "theme.json"
private const val WALLPAPER_RESOURCE_PATH = "wallpaper.jpg"
private val ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]{1,79}")
private val HASH_PATTERN = Regex("[a-fA-F0-9]{64}")
private val ALLOWED_ENGINES = setOf("miuix", "material", "liquid_glass", "snow", "rain", "pixel", "skrootpro", "alpha")
private val ALLOWED_VARIANTS = mapOf(
    "snow" to setOf("spring", "summer", "autumn", "winter"),
    "rain" to setOf("light_rain", "medium_rain", "heavy_rain", "thunderstorm", "after_rain"),
    "pixel" to setOf(
        "classic_handheld", "neon_arcade", "pastoral_fields", "star_voyage", "ink_jade",
        "rust_wasteland", "ocean_depths", "cyber_hacker", "three_kingdoms", "bianliang_market",
        "fishing_harbor", "tribal_jungle", "lava_valley", "dunhuang_desert", "viking_snowfield",
        "jiangnan_watertown", "cloud_town",
    ),
)
private val ALLOWED_GITHUB_HOSTS = setOf("raw.githubusercontent.com", "github.com", "objects.githubusercontent.com")
private val ALLOWED_PROXY_HOSTS = setOf("ghproxy.net")
private val ALLOWED_BUNDLE_PATHS = setOf(BUNDLE_MANIFEST_PATH, THEME_RESOURCE_PATH, WALLPAPER_RESOURCE_PATH)

internal data class InterfaceStyleBundle(
    val manifest: ByteArray,
    val resources: Map<String, ByteArray>,
)

private data class InterfaceStyleResource(
    val name: String,
    val path: String,
    val mimeType: String,
    val sha256: String,
    val sizeBytes: Long,
)

internal const val INTERFACE_STYLE_RESULT_KEY = "interface_style_store_result"

fun interfaceStyleCatalogUrl(): String = DEFAULT_CATALOG_URL

fun readInterfaceStyleDownloadPreferences(context: Context): InterfaceStyleDownloadPreferences {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return InterfaceStyleDownloadPreferences(
        mode = runCatching { InterfaceStyleProxyMode.valueOf(prefs.getString(MODE_KEY, null).orEmpty()) }
            .getOrDefault(InterfaceStyleProxyMode.Auto),
        customProxy = prefs.getString(CUSTOM_PROXY_KEY, "").orEmpty().trim().take(240),
    )
}

fun saveInterfaceStyleDownloadPreferences(
    context: Context,
    preferences: InterfaceStyleDownloadPreferences,
) {
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(MODE_KEY, preferences.mode.name)
        putString(CUSTOM_PROXY_KEY, preferences.customProxy.trim().take(240))
    }
}

class InterfaceStyleRegistry(context: Context) {
    internal companion object Changes {
        private val stateGeneration = AtomicLong(0L)
        private val stateChanges = MutableStateFlow(0L)
        val changes = stateChanges.asStateFlow()
    }

    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "interface-styles").apply { mkdirs() }
    private val bundlesDirectory = File(directory, BUNDLES_DIRECTORY_NAME).apply { mkdirs() }
    private val stateFile get() = File(directory, STATE_NAME)
    private var cachedTimestamp = Long.MIN_VALUE
    private var cachedLength = Long.MIN_VALUE
    private var cachedStyles: List<InstalledInterfaceStyle> = emptyList()
    private var cachedGeneration = Long.MIN_VALUE

    @Synchronized
    fun list(): List<InstalledInterfaceStyle> = readState()
        .sortedWith(compareBy<InstalledInterfaceStyle> { it.style.name.lowercase(Locale.ROOT) }.thenBy { it.style.id })

    @Synchronized
    fun get(id: String): InstalledInterfaceStyle? = readState().firstOrNull { it.style.id == id }

    @Synchronized
    internal fun install(style: InterfaceStylePackage, bundle: InterfaceStyleBundle): InstalledInterfaceStyle {
        validatePackage(style)
        val staging = File(bundlesDirectory, ".${style.id}.${UUID.randomUUID()}.tmp")
        val active = File(bundlesDirectory, style.id)
        val backup = File(bundlesDirectory, ".${style.id}.${UUID.randomUUID()}.bak")
        require(staging.mkdirs()) { "Cannot create interface style staging directory" }
        try {
            writeBundleFile(staging, BUNDLE_MANIFEST_PATH, bundle.manifest)
            bundle.resources.forEach { (path, bytes) -> writeBundleFile(staging, path, bytes) }
            require(isBundleComplete(style, staging)) { "Interface style resources are incomplete" }

            if (active.exists()) {
                require(active.renameTo(backup)) { "Cannot preserve the previous interface style" }
            }
            try {
                require(staging.renameTo(active)) { "Cannot activate interface style resources" }
                val installed = InstalledInterfaceStyle(style, System.currentTimeMillis())
                val updated = readState().filterNot { it.style.id == style.id } + installed
                try {
                    writeState(updated)
                } catch (error: Throwable) {
                    active.deleteRecursively()
                    if (backup.exists()) backup.renameTo(active)
                    throw error
                }
                backup.deleteRecursively()
                return installed
            } catch (error: Throwable) {
                if (!active.exists() && backup.exists()) backup.renameTo(active)
                throw error
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    @Synchronized
    fun remove(id: String): Boolean {
        val current = readState()
        val updated = current.filterNot { it.style.id == id }
        if (updated.size == current.size) return false
        writeState(updated)
        File(bundlesDirectory, id).deleteRecursively()
        return true
    }

    @Synchronized
    fun resourceFile(id: String, path: String): File? {
        require(ID_PATTERN.matches(id)) { "Interface style id is invalid" }
        require(path in ALLOWED_BUNDLE_PATHS && path != BUNDLE_MANIFEST_PATH) {
            "Interface style resource path is invalid"
        }
        if (readState().none { it.style.id == id }) return null
        return File(File(bundlesDirectory, id), path).takeIf { it.isFile }
    }

    @Synchronized
    fun theme(engine: String, variant: String?): InterfaceStyleTheme? {
        val installed = readState().firstOrNull {
            it.style.engine == engine && it.style.variant == variant
        } ?: return null
        val themeFile = resourceFile(installed.style.id, THEME_RESOURCE_PATH) ?: return null
        return runCatching {
            val bytes = themeFile.inputStream().use { it.readLimited(MAX_BUNDLE_ENTRY_BYTES) }
            parseInterfaceStyleTheme(bytes, installed.style)
        }.getOrNull()
    }

    private fun readState(): List<InstalledInterfaceStyle> {
        if (!stateFile.isFile) {
            cachedTimestamp = Long.MIN_VALUE
            cachedLength = Long.MIN_VALUE
            cachedStyles = emptyList()
            cachedGeneration = stateGeneration.get()
            return emptyList()
        }
        val generation = stateGeneration.get()
        val timestamp = stateFile.lastModified()
        val length = stateFile.length()
        if (generation == cachedGeneration && timestamp == cachedTimestamp && length == cachedLength) {
            return cachedStyles
        }
        val styles = runCatching {
            val root = JSONObject(AtomicFile(stateFile).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
            require(root.optString("schema") == INTERFACE_STYLE_STATE_SCHEMA)
            require(root.optInt("version") == INTERFACE_STYLE_VERSION)
            val items = root.optJSONArray("styles") ?: JSONArray()
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    runCatching {
                        val style = parseStyle(item, requireUrl = false)
                        val installed = InstalledInterfaceStyle(style, item.optLong("installedAt", 0L).coerceAtLeast(0L))
                        if (isBundleComplete(style, File(bundlesDirectory, style.id))) add(installed)
                    }
                }
            }
        }.getOrDefault(emptyList())
        cachedTimestamp = stateFile.lastModified()
        cachedLength = stateFile.length()
        cachedStyles = styles
        cachedGeneration = generation
        return styles
    }

    private fun writeState(styles: List<InstalledInterfaceStyle>) {
        val root = JSONObject()
            .put("schema", INTERFACE_STYLE_STATE_SCHEMA)
            .put("version", INTERFACE_STYLE_VERSION)
            .put("styles", JSONArray().also { array ->
                styles.forEach { installed ->
                    array.put(styleJson(installed.style).put("installedAt", installed.installedAt))
                }
            })
        val atomicFile = AtomicFile(stateFile)
        val output = atomicFile.startWrite()
        try {
            output.write(root.toString().toByteArray(Charsets.UTF_8))
            output.fd.sync()
            atomicFile.finishWrite(output)
            val generation = stateGeneration.incrementAndGet()
            stateChanges.value = generation
            cachedGeneration = generation
            cachedTimestamp = stateFile.lastModified()
            cachedLength = stateFile.length()
            cachedStyles = styles
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun writeBundleFile(parent: File, path: String, bytes: ByteArray) {
        require(path in ALLOWED_BUNDLE_PATHS) { "Interface style resource path is invalid" }
        val target = File(parent, path)
        FileOutputStream(target).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
    }

    private fun isBundleComplete(style: InterfaceStylePackage, bundleDirectory: File): Boolean {
        return runCatching {
            validatePackage(style)
            val requiredPath = if (style.engine == "snow") WALLPAPER_RESOURCE_PATH else THEME_RESOURCE_PATH
            require(bundleDirectory.isDirectory)
            require(bundleDirectory.list()?.toSet() == setOf(BUNDLE_MANIFEST_PATH, requiredPath))
            val manifestFile = File(bundleDirectory, BUNDLE_MANIFEST_PATH)
            require(manifestFile.length() in 1..MAX_BUNDLE_ENTRY_BYTES)
            val manifestBytes = manifestFile.inputStream().use { it.readLimited(MAX_BUNDLE_ENTRY_BYTES) }
            val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
            require(manifest.optString("schema") == INTERFACE_STYLE_BUNDLE_SCHEMA)
            require(manifest.optInt("version") == style.version)
            require(manifest.optString("id") == style.id)
            require(manifest.optString("engine") == style.engine)
            require(manifest.optString("variant").takeIf(String::isNotBlank) == style.variant)

            val resource = parseBundleResources(manifest.optJSONArray("resources") ?: JSONArray()).single()
            require(resource.path == requiredPath)
            val resourceFile = File(bundleDirectory, requiredPath)
            require(resourceFile.isFile && resourceFile.length() == resource.sizeBytes)
            val digest = MessageDigest.getInstance("SHA-256")
            resourceFile.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            require(digest.digest().toHexString() == resource.sha256)

            if (requiredPath == WALLPAPER_RESOURCE_PATH) {
                require(resource.name == "wallpaper" && resource.mimeType == "image/jpeg")
                validateWallpaperFile(resourceFile)
            } else {
                require(resource.name == "theme" && resource.mimeType == "application/json")
                val theme = resourceFile.inputStream().use { it.readLimited(MAX_BUNDLE_ENTRY_BYTES) }
                validateThemeResource(theme, style)
            }
            true
        }.getOrDefault(false)
    }
}

fun interfaceStyleWallpaperFile(context: Context, season: String): File? {
    val id = "season-$season"
    return InterfaceStyleRegistry(context).resourceFile(id, WALLPAPER_RESOURCE_PATH)
}

fun interfaceStyleTheme(
    context: Context,
    engine: String,
    variant: String? = null,
): InterfaceStyleTheme? = InterfaceStyleRegistry(context).theme(engine, variant)

class InterfaceStyleCatalogRepository(
    context: Context,
    private val client: okhttp3.OkHttpClient = ksuApp.okhttpClient,
) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, "interface-styles/$CATALOG_CACHE_NAME")
    private val cacheSignatureFile = File(appContext.filesDir, "interface-styles/$CATALOG_SIGNATURE_CACHE_NAME")

    suspend fun fetch(forceNetwork: Boolean = false): InterfaceStyleCatalogSnapshot = withContext(Dispatchers.IO) {
        val cached = readCatalogFile(cacheFile, cacheSignatureFile)
        try {
            if (!forceNetwork && cached != null) {
                return@withContext InterfaceStyleCatalogSnapshot(cached, InterfaceStyleCatalogSource.Cache, offline = true)
            }
            val catalogBytes = downloadCatalogFile(DEFAULT_CATALOG_URL, MAX_CATALOG_BYTES, "application/json")
            val signatureBytes = downloadCatalogFile(DEFAULT_CATALOG_SIGNATURE_URL, MAX_SIGNATURE_BYTES, "text/plain")
            verifyInterfaceStyleCatalogSignature(catalogBytes, signatureBytes)
            val catalog = parseInterfaceStyleCatalogLenient(catalogBytes.toString(Charsets.UTF_8))
            cacheFile.parentFile?.mkdirs()
            writeAtomic(cacheSignatureFile, signatureBytes)
            writeAtomic(cacheFile, catalogBytes)
            InterfaceStyleCatalogSnapshot(catalog, InterfaceStyleCatalogSource.Network, offline = false)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            cached?.let {
                return@withContext InterfaceStyleCatalogSnapshot(
                    catalog = it,
                    source = InterfaceStyleCatalogSource.Cache,
                    offline = true,
                    errorMessage = error.safeInterfaceStyleMessage(),
                )
            }
            val bundled = runCatching { readBundledCatalog() }
                .getOrElse { InterfaceStyleCatalog(generatedAt = 0L, styles = emptyList()) }
            InterfaceStyleCatalogSnapshot(
                catalog = bundled,
                source = InterfaceStyleCatalogSource.Bundled,
                offline = true,
                errorMessage = error.safeInterfaceStyleMessage(),
            )
        }
    }

    private fun readCatalogFile(file: File, signatureFile: File): InterfaceStyleCatalog? = if (file.isFile && signatureFile.isFile) {
        runCatching {
            val catalog = AtomicFile(file).openRead().use { it.readLimited(MAX_CATALOG_BYTES) }
            val signature = AtomicFile(signatureFile).openRead().use { it.readLimited(MAX_SIGNATURE_BYTES) }
            verifyInterfaceStyleCatalogSignature(catalog, signature)
            parseInterfaceStyleCatalogLenient(catalog.toString(Charsets.UTF_8))
        }
            .getOrNull()
    } else null

    private fun readBundledCatalog(): InterfaceStyleCatalog {
        val catalog = appContext.assets.open(CATALOG_ASSET).use { it.readLimited(MAX_CATALOG_BYTES) }
        val signature = appContext.assets.open(CATALOG_SIGNATURE_ASSET).use { it.readLimited(MAX_SIGNATURE_BYTES) }
        verifyBundledInterfaceStyleCatalogSignature(catalog, signature)
        return parseInterfaceStyleCatalogLenient(catalog.toString(Charsets.UTF_8))
    }

    private fun downloadCatalogFile(url: String, maximumBytes: Long, accept: String): ByteArray {
        val request = Request.Builder().url(validateStyleUrl(url)).header("Accept", accept).get().build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Interface style catalog HTTP ${response.code}" }
            require(response.request.url.scheme == "https" && isAllowedStyleHost(response.request.url.host)) {
                "Interface style catalog redirected to an unsupported host"
            }
            return response.body.byteStream().readLimited(maximumBytes)
        }
    }

    private fun writeAtomic(file: File, bytes: ByteArray) {
        val atomicFile = AtomicFile(file)
        val output = atomicFile.startWrite()
        try {
            output.write(bytes)
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }
}

class InterfaceStyleInstaller(
    context: Context,
    private val registry: InterfaceStyleRegistry = InterfaceStyleRegistry(context),
    private val client: okhttp3.OkHttpClient = ksuApp.okhttpClient,
) {
    private val appContext = context.applicationContext

    suspend fun install(
        style: InterfaceStylePackage,
        preferences: InterfaceStyleDownloadPreferences = readInterfaceStyleDownloadPreferences(appContext),
        onProgress: (InterfaceStyleDownloadProgress) -> Unit = {},
    ): InstalledInterfaceStyle = withContext(Dispatchers.IO) {
        validatePackage(style)
        require(style.sizeBytes in 1..MAX_PACKAGE_BYTES) { "Interface style package size is invalid" }
        var lastError: Throwable? = null
        for (url in resolveInterfaceStyleUrls(style.downloadUrl, preferences)) {
            try {
                return@withContext downloadAndInstall(url, style, onProgress)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("Interface style download failed")
    }

    private fun downloadAndInstall(
        url: String,
        style: InterfaceStylePackage,
        onProgress: (InterfaceStyleDownloadProgress) -> Unit,
    ): InstalledInterfaceStyle {
        val initialHost = URI(url).host.lowercase(Locale.ROOT)
        val request = Request.Builder().url(url).header("Accept", "application/octet-stream").build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Interface style download HTTP ${response.code}" }
            val responseHost = response.request.url.host.lowercase(Locale.ROOT)
            require(response.request.url.scheme == "https" &&
                (responseHost == initialHost || isAllowedStyleHost(responseHost))) {
                "Interface style download redirected to an unsupported host"
            }
            val declared = response.body.contentLength()
            require(declared < 0L || declared == style.sizeBytes) { "Interface style byte count does not match catalog" }
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = response.body.byteStream().readLimited(MAX_PACKAGE_BYTES, onProgress, style.sizeBytes, digest)
            require(bytes.size.toLong() == style.sizeBytes) { "Interface style package is incomplete" }
            val actualHash = digest.digest().toHexString()
            require(actualHash.equals(style.sha256, ignoreCase = true)) { "Interface style SHA-256 verification failed" }
            val bundle = parseInterfaceStyleBundle(bytes, style)
            validateBundleImages(bundle)
            return registry.install(style, bundle)
        }
    }

    private fun validateBundleImages(bundle: InterfaceStyleBundle) {
        val wallpaper = bundle.resources[WALLPAPER_RESOURCE_PATH] ?: return
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(wallpaper, 0, wallpaper.size, options)
        validateWallpaperBounds(options)
    }
}

private fun validateWallpaperFile(file: File) {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    validateWallpaperBounds(options)
}

private fun validateWallpaperBounds(options: BitmapFactory.Options) {
    require(options.outWidth in 1..MAX_IMAGE_DIMENSION && options.outHeight in 1..MAX_IMAGE_DIMENSION) {
        "Interface style wallpaper dimensions are invalid"
    }
    require(options.outWidth.toLong() * options.outHeight.toLong() <= MAX_IMAGE_PIXELS) {
        "Interface style wallpaper is too large"
    }
    require(options.outMimeType == "image/jpeg") { "Interface style wallpaper must be JPEG" }
}

internal fun parseInterfaceStyleBundle(
    bytes: ByteArray,
    expected: InterfaceStylePackage,
): InterfaceStyleBundle {
    require(bytes.size.toLong() in 1..MAX_PACKAGE_BYTES) { "Interface style package size is invalid" }
    val entries = linkedMapOf<String, ByteArray>()
    var totalUncompressed = 0L
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            require(!entry.isDirectory && entry.name in ALLOWED_BUNDLE_PATHS) {
                "Interface style package contains an unsupported entry"
            }
            require(entry.name !in entries) { "Interface style package contains duplicate entries" }
            if (entry.size >= 0L) {
                require(entry.size <= MAX_BUNDLE_ENTRY_BYTES) { "Interface style resource is too large" }
            }
            val entryBytes = zip.readLimited(MAX_BUNDLE_ENTRY_BYTES)
            totalUncompressed += entryBytes.size
            require(totalUncompressed <= MAX_BUNDLE_UNCOMPRESSED_BYTES) {
                "Interface style package expands beyond the allowed size"
            }
            entries[entry.name] = entryBytes
            zip.closeEntry()
        }
    }

    val manifestBytes = requireNotNull(entries.remove(BUNDLE_MANIFEST_PATH)) {
        "Interface style package has no manifest"
    }
    val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
    require(manifest.optString("schema") == INTERFACE_STYLE_BUNDLE_SCHEMA) {
        "Unsupported interface style package"
    }
    require(manifest.optInt("version") == expected.version) { "Interface style package version does not match catalog" }
    require(manifest.optString("id") == expected.id &&
        manifest.optString("engine") == expected.engine &&
        manifest.optString("variant").takeIf(String::isNotBlank) == expected.variant) {
        "Interface style package metadata does not match catalog"
    }

    val declaredResources = parseBundleResources(manifest.optJSONArray("resources") ?: JSONArray())
    require(declaredResources.map { it.path }.toSet() == entries.keys) {
        "Interface style package resource list does not match its contents"
    }
    declaredResources.forEach { resource ->
        val resourceBytes = requireNotNull(entries[resource.path])
        require(resourceBytes.size.toLong() == resource.sizeBytes) { "Interface style resource size mismatch" }
        val actualHash = MessageDigest.getInstance("SHA-256").digest(resourceBytes).toHexString()
        require(actualHash.equals(resource.sha256, ignoreCase = true)) { "Interface style resource hash mismatch" }
    }

    val requiredResource = if (expected.engine == "snow") WALLPAPER_RESOURCE_PATH else THEME_RESOURCE_PATH
    require(entries.keys == setOf(requiredResource)) { "Interface style package resources are invalid for this engine" }
    if (requiredResource == WALLPAPER_RESOURCE_PATH) {
        val resource = declaredResources.single()
        require(resource.name == "wallpaper" && resource.mimeType == "image/jpeg") {
            "Interface style wallpaper declaration is invalid"
        }
    } else {
        val resource = declaredResources.single()
        require(resource.name == "theme" && resource.mimeType == "application/json") {
            "Interface style theme declaration is invalid"
        }
        validateThemeResource(entries.getValue(THEME_RESOURCE_PATH), expected)
    }
    return InterfaceStyleBundle(manifestBytes, entries)
}

private fun parseBundleResources(items: JSONArray): List<InterfaceStyleResource> {
    require(items.length() == 1) { "Interface style package must contain exactly one resource" }
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: error("Interface style resource declaration is invalid")
            val path = item.optString("path")
            require(path in setOf(THEME_RESOURCE_PATH, WALLPAPER_RESOURCE_PATH)) {
                "Interface style resource path is invalid"
            }
            add(
                InterfaceStyleResource(
                    name = item.optString("name"),
                    path = path,
                    mimeType = item.optString("mimeType"),
                    sha256 = item.optString("sha256").lowercase(Locale.ROOT).also {
                        require(HASH_PATTERN.matches(it)) { "Interface style resource hash is invalid" }
                    },
                    sizeBytes = item.optLong("sizeBytes").also {
                        require(it in 1..MAX_BUNDLE_ENTRY_BYTES) { "Interface style resource size is invalid" }
                    },
                )
            )
        }
    }
}

private fun validateThemeResource(bytes: ByteArray, expected: InterfaceStylePackage) {
    parseInterfaceStyleTheme(bytes, expected)
}

internal fun parseInterfaceStyleCatalog(json: String): InterfaceStyleCatalog {
    require(json.toByteArray(Charsets.UTF_8).size <= MAX_CATALOG_BYTES) { "Interface style catalog is too large" }
    val root = JSONObject(json)
    require(root.optString("schema") == INTERFACE_STYLE_SCHEMA) { "Unsupported interface style catalog" }
    require(root.optInt("version") == INTERFACE_STYLE_VERSION) { "Unsupported interface style catalog version" }
    val stylesJson = root.optJSONArray("styles") ?: error("Interface style catalog has no styles")
    require(stylesJson.length() <= 64) { "Interface style catalog has too many styles" }
    val styles = buildList {
        for (index in 0 until stylesJson.length()) {
            val item = stylesJson.optJSONObject(index) ?: error("Interface style entry is invalid")
            add(parseStyle(item, requireUrl = true))
        }
    }
    require(styles.distinctBy { it.id }.size == styles.size) { "Interface style ids must be unique" }
    return InterfaceStyleCatalog(root.optLong("generatedAt", 0L).coerceAtLeast(0L), styles)
}

/**
 * Parses a signed catalog for display. A bad optional entry must not take down
 * the manager, so entries that fail the same validation as strict parsing are
 * omitted while the rest of the catalog remains usable.
 */
internal fun parseInterfaceStyleCatalogLenient(json: String): InterfaceStyleCatalog {
    require(json.toByteArray(Charsets.UTF_8).size <= MAX_CATALOG_BYTES) { "Interface style catalog is too large" }
    val root = JSONObject(json)
    require(root.optString("schema") == INTERFACE_STYLE_SCHEMA) { "Unsupported interface style catalog" }
    require(root.optInt("version") == INTERFACE_STYLE_VERSION) { "Unsupported interface style catalog version" }
    val stylesJson = root.optJSONArray("styles") ?: error("Interface style catalog has no styles")
    require(stylesJson.length() <= 64) { "Interface style catalog has too many styles" }
    val styles = buildList {
        for (index in 0 until stylesJson.length()) {
            val item = stylesJson.optJSONObject(index) ?: continue
            runCatching { parseStyle(item, requireUrl = true) }
                .onSuccess { style -> add(style) }
        }
    }
    return InterfaceStyleCatalog(
        generatedAt = root.optLong("generatedAt", 0L).coerceAtLeast(0L),
        styles = styles.distinctBy { it.id },
    )
}

private fun parseStyle(item: JSONObject, requireUrl: Boolean): InterfaceStylePackage {
    val id = item.optString("id")
    require(ID_PATTERN.matches(id)) { "Interface style id is invalid" }
    val engine = item.optString("engine")
    require(engine in ALLOWED_ENGINES) { "Interface style engine is invalid" }
    val variant = item.optString("variant").takeIf { it.isNotBlank() }
    val allowedVariants = ALLOWED_VARIANTS[engine]
    require(if (allowedVariants == null) variant == null else variant in allowedVariants) {
        "Interface style variant is invalid"
    }
    val url = migrateCloudThemeStoreUrl(item.optString("downloadUrl"))
    if (requireUrl) require(validateStyleUrl(url) == url) { "Interface style URL is invalid" }
    val hash = item.optString("sha256")
    if (requireUrl) require(HASH_PATTERN.matches(hash)) { "Interface style hash is invalid" }
    return InterfaceStylePackage(
        id = id,
        name = item.optString("name").trim().take(80).also { require(it.isNotBlank()) },
        summary = item.optString("summary").trim().take(240),
        engine = engine,
        variant = variant,
        version = item.optInt("version", INTERFACE_STYLE_VERSION).also { require(it in 1..1000) },
        downloadUrl = url,
        sha256 = hash.lowercase(Locale.ROOT),
        sizeBytes = item.optLong("sizeBytes", 0L).also { require(it in 1..MAX_PACKAGE_BYTES) },
        accent = item.optLong("accent", 0xFF6750A4L).also {
            require(it in 0L..0xFFFFFFFFL) { "Interface style accent is invalid" }
        },
    )
}

private fun styleJson(style: InterfaceStylePackage): JSONObject = JSONObject()
    .put("schema", INTERFACE_STYLE_STATE_SCHEMA)
    .put("version", style.version)
    .put("id", style.id)
    .put("name", style.name)
    .put("summary", style.summary)
    .put("engine", style.engine)
    .put("variant", style.variant)
    .put("downloadUrl", style.downloadUrl)
    .put("sha256", style.sha256)
    .put("sizeBytes", style.sizeBytes)
    .put("accent", style.accent)

private fun validatePackage(style: InterfaceStylePackage) {
    require(ID_PATTERN.matches(style.id))
    require(style.engine in ALLOWED_ENGINES)
    val variants = ALLOWED_VARIANTS[style.engine]
    require(if (variants == null) style.variant == null else style.variant in variants)
    require(style.version in 1..1000)
    require(HASH_PATTERN.matches(style.sha256))
    require(style.sizeBytes in 1..MAX_PACKAGE_BYTES)
}

private fun validateStyleUrl(raw: String): String {
    val uri = URI(raw)
    require(uri.scheme.equals("https", ignoreCase = true) && uri.userInfo == null && uri.fragment == null) {
        "Interface style URL must use HTTPS"
    }
    require(uri.host != null && isAllowedStyleHost(uri.host)) { "Interface style URL host is not allowed" }
    return raw
}

private fun isAllowedStyleHost(host: String): Boolean = host.lowercase(Locale.ROOT) in ALLOWED_GITHUB_HOSTS

private fun validateStyleProxyUrl(raw: String): String {
    val uri = URI(raw)
    require(uri.scheme.equals("https", ignoreCase = true) && uri.userInfo == null && uri.fragment == null)
    require(uri.host != null && uri.host.lowercase(Locale.ROOT) in ALLOWED_PROXY_HOSTS)
    return raw
}

internal fun resolveInterfaceStyleUrls(
    raw: String,
    preferences: InterfaceStyleDownloadPreferences,
): List<String> {
    val original = validateStyleUrl(raw)
    return when (preferences.mode) {
        InterfaceStyleProxyMode.Direct -> listOf(original)
        InterfaceStyleProxyMode.Auto -> {
            val uri = URI(original)
            listOf("https://ghproxy.net/${uri}".also(::validateStyleProxyUrl), original)
        }
        InterfaceStyleProxyMode.Custom -> {
            val proxy = preferences.customProxy.trim().removeSuffix("/")
            require(proxy.isNotBlank()) { "Custom proxy URL is empty" }
            val proxyUri = URI(proxy)
            require(proxyUri.scheme.equals("https", ignoreCase = true) && proxyUri.userInfo == null) {
                "Custom proxy must use HTTPS"
            }
            val path = if (proxyUri.path.endsWith("/")) proxyUri.path.dropLast(1) else proxyUri.path
            val rewritten = URI(proxyUri.scheme, proxyUri.authority, "$path/${original}", null, null).toString()
            // Custom hosts are intentionally limited to the user-entered HTTPS proxy host.
            require(URI(rewritten).scheme.equals("https", ignoreCase = true))
            listOf(rewritten)
        }
    }
}

internal fun verifyInterfaceStyleCatalogSignature(
    catalog: ByteArray,
    signatureText: ByteArray,
    publicKeyBase64: String = CATALOG_PUBLIC_KEY_B64,
) {
    val signatureBytes = Base64.getMimeDecoder().decode(signatureText.toString(Charsets.UTF_8).trim())
    require(signatureBytes.size == 64) { "Interface style catalog signature is invalid" }
    val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
    val publicKey = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(keyBytes))
    val verifier = Signature.getInstance("Ed25519")
    verifier.initVerify(publicKey)
    verifier.update(catalog)
    require(verifier.verify(signatureBytes)) { "Interface style catalog signature verification failed" }
}

private fun verifyBundledInterfaceStyleCatalogSignature(catalog: ByteArray, signature: ByteArray) {
    try {
        verifyInterfaceStyleCatalogSignature(catalog, signature)
    } catch (_: NoSuchAlgorithmException) {
        // Bundled assets are protected by the signed APK. Some vendor images
        // omit the Ed25519 provider, so keep the local catalog usable without
        // weakening verification for downloaded catalogs.
    }
}

private fun java.io.InputStream.readLimited(
    maxBytes: Long,
    onProgress: ((InterfaceStyleDownloadProgress) -> Unit)? = null,
    total: Long = 0L,
    digest: MessageDigest? = null,
): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var copied = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        copied += read
        require(copied <= maxBytes) { "Interface style response is too large" }
        output.write(buffer, 0, read)
        digest?.update(buffer, 0, read)
        onProgress?.invoke(InterfaceStyleDownloadProgress(copied, total))
    }
    return output.toByteArray()
}

private fun ByteArray.toHexString(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

internal fun Throwable.safeInterfaceStyleMessage(): String = localizedMessage
    ?.trim()
    ?.lineSequence()
    ?.firstOrNull()
    ?.take(240)
    ?.takeIf { it.isNotBlank() }
    ?: javaClass.simpleName
