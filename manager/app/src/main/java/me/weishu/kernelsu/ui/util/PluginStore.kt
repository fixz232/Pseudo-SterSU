package me.weishu.kernelsu.ui.util

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.ksuApp
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Base64
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Downloadable Manager plugins are declarative capability grants. They cannot
 * contain code, native libraries, shell scripts, or web content. A package only
 * enables an allowlisted feature that is already covered by the Manager/ksud API.
 */
enum class ManagerPlugin(
    val id: String,
    val slots: Set<PluginSlot>,
) {
    RescueProtection("rescue-protection", setOf(PluginSlot.ToolboxRescue)),
    ImageTools("image-tools", setOf(PluginSlot.ToolboxImageTools)),
    CpuSpoof("cpu-spoof", setOf(PluginSlot.ToolboxCpuSpoof)),
    DeviceIdentity("device-identity", setOf(PluginSlot.ToolboxDeviceIdentity)),
    GraphicsRenderer("graphics-renderer", setOf(PluginSlot.ToolboxGraphicsRenderer)),
    AiChat("ai-chat", setOf(PluginSlot.ToolboxAiChat)),
    RemoteManagementSuite(
        "remote-management-suite",
        setOf(PluginSlot.MaintenanceWebManager, PluginSlot.MaintenanceStealthMode),
    ),
    AppIdManager("app-id-manager", setOf(PluginSlot.SuperuserAppIdManager)),
    AppFreeze("app-freeze", setOf(PluginSlot.SuperuserAppFreeze)),
    PathmaskLkm("pathmask-lkm", setOf(PluginSlot.MountHidePathmaskLkm)),
    ;

    companion object {
        fun fromId(id: String): ManagerPlugin? = entries.firstOrNull { it.id == id }
    }
}

enum class PluginSlot(val id: String) {
    ToolboxRescue("toolbox.rescue"),
    ToolboxImageTools("toolbox.image-tools"),
    ToolboxCpuSpoof("toolbox.cpu-spoof"),
    ToolboxDeviceIdentity("toolbox.device-identity"),
    ToolboxGraphicsRenderer("toolbox.graphics-renderer"),
    ToolboxAiChat("toolbox.ai-chat"),
    MaintenanceWebManager("maintenance.web-manager"),
    MaintenanceStealthMode("maintenance.stealth-mode"),
    SuperuserAppIdManager("superuser.app-id-manager"),
    SuperuserAppFreeze("superuser.app-freeze"),
    MountHidePathmaskLkm("mount-hide.pathmask-lkm"),
    ;

    companion object {
        fun parse(raw: String): PluginSlot? = entries.firstOrNull { it.id == raw }
    }
}

data class ManagerPluginPackage(
    val id: String,
    val version: Int,
    val name: String,
    val summary: String,
    val description: String,
    val instructions: List<String>,
    val slots: Set<PluginSlot>,
    val minManagerVersionCode: Int,
    val minKsudVersionCode: Int,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
)

data class InstalledManagerPlugin(
    val plugin: ManagerPluginPackage,
    val installedAt: Long,
)

data class ManagerPluginCatalog(
    val generatedAt: Long,
    val plugins: List<ManagerPluginPackage>,
)

data class ManagerPluginCatalogSnapshot(
    val catalog: ManagerPluginCatalog,
    val source: PluginCatalogSource,
    val offline: Boolean,
    val errorMessage: String? = null,
    val stale: Boolean = false,
)

enum class PluginCatalogSource { Network, Cache, Bundled }

enum class PluginDownloadRoute { Direct, Accelerator }

/** Why an installed declarative plugin cannot be exposed to the current runtime. */
internal enum class ManagerPluginCompatibilityIssue {
    None,
    ManagerTooOld,
    KsudMissing,
    KsudVersionUnavailable,
    KsudTooOld,
}

internal data class ManagerPluginCompatibility(
    val issue: ManagerPluginCompatibilityIssue,
    val managerVersionCode: Int,
    val ksudVersionCode: Int?,
) {
    val isCompatible: Boolean
        get() = issue == ManagerPluginCompatibilityIssue.None
}

data class PluginDownloadProgress(
    val downloaded: Long,
    val total: Long,
) {
    val fraction: Float?
        get() = total.takeIf { it > 0L }
            ?.let { (downloaded.toDouble() / it).toFloat().coerceIn(0f, 1f) }
}

private const val PLUGIN_CATALOG_SCHEMA = "io.github.fixz.apkesu.plugin-catalog"
private const val PLUGIN_PACKAGE_SCHEMA = "io.github.fixz.apkesu.plugin"
private const val PLUGIN_SCHEMA_VERSION = 1
private const val CATALOG_ASSET = "plugin-store/catalog-v1.json"
private const val SIGNATURE_ASSET = "plugin-store/catalog-v1.sig"
private const val CATALOG_CACHE_NAME = "catalog-v2.json"
private const val SIGNATURE_CACHE_NAME = "catalog-v2.sig"
private const val STATE_FILE_NAME = "installed-v1.json"
private const val MAX_CATALOG_BYTES = 384L * 1024L
private const val MAX_SIGNATURE_BYTES = 1024L
private const val MAX_PACKAGE_BYTES = 96L * 1024L
private const val CATALOG_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L
private const val MAX_DESCRIPTION_LENGTH = 2_000
private const val MAX_INSTRUCTION_COUNT = 16
private const val MAX_INSTRUCTION_LENGTH = 360
private const val ACTIVE_PLUGIN_STORE_BASE_URL =
    "https://raw.githubusercontent.com/fixz232/Pseudo-SterSU/main/plugin-store"
private const val LEGACY_PLUGIN_STORE_BASE_URL =
    "https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main"
private const val DEFAULT_CATALOG_URL =
    "$ACTIVE_PLUGIN_STORE_BASE_URL/catalog-v2.json"

// This is an X.509 SubjectPublicKeyInfo encoding for the release catalog key.
// The corresponding private key is deliberately kept outside source control.
private const val CATALOG_PUBLIC_KEY_B64 = "MCowBQYDK2VwAyEAsuUUb5hSL7V2e89TyM0XRJ9IKY6VSOqP9a5a/OMeCts="

private val PLUGIN_ID_PATTERN = Regex("[a-z][a-z0-9-]{1,63}")
private val HASH_PATTERN = Regex("[a-fA-F0-9]{64}")
private val ALLOWED_GITHUB_HOSTS = setOf(
    "raw.githubusercontent.com",
    "github.com",
    "objects.githubusercontent.com",
    "release-assets.githubusercontent.com",
)
private const val ACCELERATOR_HOST = "ghproxy.net"

internal const val PLUGIN_STORE_RESULT_KEY = "plugin_store_result"

fun managerPluginCatalogUrl(): String = DEFAULT_CATALOG_URL

fun ManagerPlugin.isInstalled(context: Context = ksuApp): Boolean =
    ManagerPluginRegistry(context).contains(id)

fun Set<String>.hasPlugin(plugin: ManagerPlugin): Boolean = plugin.id in this

class ManagerPluginRegistry(context: Context) {
    private companion object {
        val stateGeneration = AtomicLong(0L)
    }

    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "manager-plugins").apply { mkdirs() }
    private val stateFile get() = File(directory, STATE_FILE_NAME)
    private var cachedGeneration = Long.MIN_VALUE
    private var cachedModified = Long.MIN_VALUE
    private var cachedLength = Long.MIN_VALUE
    private var cachedPlugins: List<InstalledManagerPlugin> = emptyList()

    @Synchronized
    fun list(): List<InstalledManagerPlugin> = readState().sortedBy { it.plugin.id }

    @Synchronized
    fun ids(): Set<String> = readState().mapTo(linkedSetOf()) { it.plugin.id }

    @Synchronized
    fun contains(id: String): Boolean = readState().any { it.plugin.id == id }

    @Synchronized
    fun install(plugin: ManagerPluginPackage): InstalledManagerPlugin {
        validatePlugin(plugin, requireDownload = false)
        require(plugin.minManagerVersionCode <= BuildConfig.VERSION_CODE) {
            "Plugin requires Manager ${plugin.minManagerVersionCode}"
        }
        val installed = InstalledManagerPlugin(plugin, System.currentTimeMillis())
        writeState(readState().filterNot { it.plugin.id == plugin.id } + installed)
        return installed
    }

    @Synchronized
    fun remove(id: String): Boolean {
        val before = readState()
        val after = before.filterNot { it.plugin.id == id }
        if (before.size == after.size) return false
        writeState(after)
        return true
    }

    private fun readState(): List<InstalledManagerPlugin> {
        if (!stateFile.isFile) {
            updateCache(emptyList())
            return emptyList()
        }
        val generation = stateGeneration.get()
        val modified = stateFile.lastModified()
        val length = stateFile.length()
        if (generation == cachedGeneration && modified == cachedModified && length == cachedLength) {
            return cachedPlugins
        }
        val loaded = runCatching {
            val root = AtomicFile(stateFile).openRead().bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            require(root.optString("schema") == PLUGIN_PACKAGE_SCHEMA)
            val items = root.optJSONArray("plugins") ?: JSONArray()
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    runCatching {
                        val plugin = parsePlugin(item, requireDownload = false)
                        add(InstalledManagerPlugin(plugin, item.optLong("installedAt").coerceAtLeast(0L)))
                    }
                }
            }.distinctBy { it.plugin.id }
        }.getOrDefault(emptyList())
        updateCache(loaded)
        return loaded
    }

    private fun writeState(plugins: List<InstalledManagerPlugin>) {
        val root = JSONObject()
            .put("schema", PLUGIN_PACKAGE_SCHEMA)
            .put("version", PLUGIN_SCHEMA_VERSION)
            .put("plugins", JSONArray().also { values ->
                plugins.sortedBy { it.plugin.id }.forEach { installed ->
                    values.put(pluginJson(installed.plugin).put("installedAt", installed.installedAt))
                }
            })
        val target = AtomicFile(stateFile)
        val output = target.startWrite()
        try {
            output.write(root.toString().toByteArray(Charsets.UTF_8))
            output.fd.sync()
            target.finishWrite(output)
            stateGeneration.incrementAndGet()
            updateCache(plugins)
        } catch (error: Throwable) {
            target.failWrite(output)
            throw error
        }
    }

    private fun updateCache(plugins: List<InstalledManagerPlugin>) {
        cachedGeneration = stateGeneration.get()
        cachedModified = stateFile.lastModified()
        cachedLength = stateFile.length()
        cachedPlugins = plugins
    }
}

class ManagerPluginCatalogRepository(
    context: Context,
    private val client: okhttp3.OkHttpClient = ksuApp.okhttpClient,
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "manager-plugins").apply { mkdirs() }
    private val catalogCache = File(directory, CATALOG_CACHE_NAME)
    private val signatureCache = File(directory, SIGNATURE_CACHE_NAME)

    suspend fun fetch(
        forceNetwork: Boolean = false,
        route: PluginDownloadRoute = PluginDownloadRoute.Accelerator,
    ): ManagerPluginCatalogSnapshot = withContext(Dispatchers.IO) {
        val cached = readCatalog(catalogCache, signatureCache)
        val cachedIsStale = cached?.let { isPluginCatalogStale(it.generatedAt) } ?: false
        if (!forceNetwork && cached != null) {
            return@withContext ManagerPluginCatalogSnapshot(
                catalog = cached,
                source = PluginCatalogSource.Cache,
                offline = true,
                stale = cachedIsStale,
            )
        }
        try {
            val catalogUrl = validatePluginUrl(DEFAULT_CATALOG_URL)
            var failure: Throwable? = null
            for ((candidateCatalogUrl, candidateSignatureUrl) in
                resolvePluginCatalogUrls(catalogUrl, route)) {
                try {
                    // Keep the catalog and its signature on the same route. A
                    // proxy can return a different revision or fail one file;
                    // mixing routes would make valid pairs look invalid.
                    val catalogBytes = download(candidateCatalogUrl, MAX_CATALOG_BYTES, "application/json")
                    val signatureBytes = download(candidateSignatureUrl, MAX_SIGNATURE_BYTES, "text/plain")
                    verifyManagerPluginCatalogSignature(catalogBytes, signatureBytes)
                    val catalog = parseManagerPluginCatalog(catalogBytes.toString(Charsets.UTF_8))
                    atomicWrite(catalogCache, catalogBytes)
                    atomicWrite(signatureCache, signatureBytes)
                    return@withContext ManagerPluginCatalogSnapshot(
                        catalog,
                        PluginCatalogSource.Network,
                        offline = false,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    failure = error
                }
            }
            throw failure ?: IllegalStateException("Plugin catalog download failed")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            cached?.let {
                return@withContext ManagerPluginCatalogSnapshot(
                    it,
                    PluginCatalogSource.Cache,
                    offline = true,
                    errorMessage = safePluginMessage(error),
                    stale = cachedIsStale,
                )
            }
            val bundled = readBundledCatalog()
            ManagerPluginCatalogSnapshot(
                bundled,
                PluginCatalogSource.Bundled,
                offline = true,
                errorMessage = safePluginMessage(error),
                stale = isPluginCatalogStale(bundled.generatedAt),
            )
        }
    }

    private fun readCatalog(catalogFile: File, signatureFile: File): ManagerPluginCatalog? {
        if (!catalogFile.isFile || !signatureFile.isFile) return null
        return runCatching {
            val catalog = AtomicFile(catalogFile).openRead().use { it.readLimited(MAX_CATALOG_BYTES) }
            val signature = AtomicFile(signatureFile).openRead().use { it.readLimited(MAX_SIGNATURE_BYTES) }
            verifyManagerPluginCatalogSignature(catalog, signature)
            parseManagerPluginCatalog(catalog.toString(Charsets.UTF_8))
        }.getOrNull()
    }

    private fun readBundledCatalog(): ManagerPluginCatalog {
        val catalog = appContext.assets.open(CATALOG_ASSET).use { it.readLimited(MAX_CATALOG_BYTES) }
        val signature = appContext.assets.open(SIGNATURE_ASSET).use { it.readLimited(MAX_SIGNATURE_BYTES) }
        verifyBundledCatalogSignature(catalog, signature)
        return parseManagerPluginCatalog(catalog.toString(Charsets.UTF_8))
    }

    private fun download(url: String, maximumBytes: Long, accept: String): ByteArray {
        val initial = URI(url).host.lowercase(Locale.ROOT)
        val request = Request.Builder().url(url).header("Accept", accept).get().build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Plugin catalog HTTP ${response.code}" }
            val redirected = response.request.url.host.lowercase(Locale.ROOT)
            require(response.request.url.scheme == "https" &&
                (redirected == initial || isAllowedPluginHost(redirected))) {
                "Plugin catalog redirected to an unsupported host"
            }
            return response.body.byteStream().readLimited(maximumBytes)
        }
    }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        val target = AtomicFile(file)
        val output = target.startWrite()
        try {
            output.write(bytes)
            output.fd.sync()
            target.finishWrite(output)
        } catch (error: Throwable) {
            target.failWrite(output)
            throw error
        }
    }
}

class ManagerPluginInstaller(
    context: Context,
    private val registry: ManagerPluginRegistry = ManagerPluginRegistry(context),
    private val client: okhttp3.OkHttpClient = ksuApp.okhttpClient,
) {
    suspend fun install(
        plugin: ManagerPluginPackage,
        route: PluginDownloadRoute = PluginDownloadRoute.Accelerator,
        onProgress: (PluginDownloadProgress) -> Unit = {},
    ): InstalledManagerPlugin = withContext(Dispatchers.IO) {
        validatePlugin(plugin, requireDownload = true)
        require(plugin.minManagerVersionCode <= BuildConfig.VERSION_CODE) {
            "Plugin requires a newer Manager"
        }
        val installedKsud = getInstalledKsudStatus()
        require(installedKsud.present &&
            installedKsud.versionCode != null &&
            installedKsud.versionCode >= plugin.minKsudVersionCode) {
            "Plugin requires installed ksud ${plugin.minKsudVersionCode} or newer"
        }
        var failure: Throwable? = null
        for (url in resolvePluginDownloadUrls(plugin.downloadUrl, route)) {
            try {
                return@withContext downloadAndInstall(plugin, url, onProgress)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                failure = error
            }
        }
        throw failure ?: IllegalStateException("Plugin download failed")
    }

    private fun downloadAndInstall(
        expected: ManagerPluginPackage,
        url: String,
        onProgress: (PluginDownloadProgress) -> Unit,
    ): InstalledManagerPlugin {
        val initialHost = URI(url).host.lowercase(Locale.ROOT)
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Plugin download HTTP ${response.code}" }
            val redirected = response.request.url.host.lowercase(Locale.ROOT)
            require(response.request.url.scheme == "https" &&
                (redirected == initialHost || isAllowedPluginHost(redirected))) {
                "Plugin download redirected to an unsupported host"
            }
            val declaredLength = response.body.contentLength()
            require(declaredLength < 0L || declaredLength <= MAX_PACKAGE_BYTES) {
                "Plugin response is too large"
            }
            val bytes = response.body.byteStream().readLimited(
                MAX_PACKAGE_BYTES,
                { read -> onProgress(PluginDownloadProgress(read, expected.sizeBytes)) },
            )
            val verifiedBytes = pluginPackageBytesMatchingCatalog(bytes, expected)
            val packageJson = JSONObject(verifiedBytes.toString(Charsets.UTF_8))
            require(packageJson.optString("schema") == PLUGIN_PACKAGE_SCHEMA) {
                "Plugin package schema is invalid"
            }
            val downloaded = parsePlugin(packageJson, requireDownload = false)
            require(pluginPackageMatchesCatalog(downloaded, expected)) {
                "Plugin package metadata does not match catalog"
            }
            return registry.install(expected)
        }
    }
}

internal fun pluginPackageMatchesCatalog(
    downloaded: ManagerPluginPackage,
    catalogEntry: ManagerPluginPackage,
): Boolean = downloaded.sha256.isEmpty() &&
    downloaded.sizeBytes == 0L &&
    downloaded == catalogEntry.copy(sha256 = "", sizeBytes = 0L)

internal fun pluginPackageBytesMatchingCatalog(
    downloaded: ByteArray,
    catalogEntry: ManagerPluginPackage,
): ByteArray {
    val payloads = listOfNotNull(downloaded, downloaded.withCrlfLineEndings())
    val sizeMatches = payloads.filter { it.size.toLong() == catalogEntry.sizeBytes }
    require(sizeMatches.isNotEmpty()) { "Plugin byte count does not match catalog" }
    return sizeMatches.firstOrNull { payload ->
        MessageDigest.getInstance("SHA-256")
            .digest(payload)
            .toHexString()
            .equals(catalogEntry.sha256, ignoreCase = true)
    } ?: throw IllegalArgumentException("Plugin SHA-256 verification failed")
}

internal fun isPluginCatalogStale(generatedAt: Long, now: Long = System.currentTimeMillis()): Boolean =
    generatedAt <= 0L ||
        generatedAt - now > 5L * 60L * 1000L ||
        now - generatedAt >= CATALOG_MAX_AGE_MILLIS

internal fun hasPluginUpdate(
    installed: InstalledManagerPlugin?,
    catalogEntry: ManagerPluginPackage,
): Boolean {
    val installedPlugin = installed?.plugin ?: return false
    return catalogEntry.version > installedPlugin.version ||
        (catalogEntry.version == installedPlugin.version &&
            !catalogEntry.sha256.equals(installedPlugin.sha256, ignoreCase = true))
}

internal fun checkManagerPluginCompatibility(
    plugin: ManagerPluginPackage,
    managerVersionCode: Int = BuildConfig.VERSION_CODE,
    ksudStatus: InstalledKsudStatus,
): ManagerPluginCompatibility {
    if (managerVersionCode < plugin.minManagerVersionCode) {
        return ManagerPluginCompatibility(
            issue = ManagerPluginCompatibilityIssue.ManagerTooOld,
            managerVersionCode = managerVersionCode,
            ksudVersionCode = ksudStatus.versionCode,
        )
    }
    if (!ksudStatus.present) {
        return ManagerPluginCompatibility(
            issue = ManagerPluginCompatibilityIssue.KsudMissing,
            managerVersionCode = managerVersionCode,
            ksudVersionCode = ksudStatus.versionCode,
        )
    }
    val ksudVersion = ksudStatus.versionCode ?: return ManagerPluginCompatibility(
        issue = ManagerPluginCompatibilityIssue.KsudVersionUnavailable,
        managerVersionCode = managerVersionCode,
        ksudVersionCode = null,
    )
    if (ksudVersion < plugin.minKsudVersionCode) {
        return ManagerPluginCompatibility(
            issue = ManagerPluginCompatibilityIssue.KsudTooOld,
            managerVersionCode = managerVersionCode,
            ksudVersionCode = ksudVersion,
        )
    }
    return ManagerPluginCompatibility(
        issue = ManagerPluginCompatibilityIssue.None,
        managerVersionCode = managerVersionCode,
        ksudVersionCode = ksudVersion,
    )
}

internal fun InstalledManagerPlugin.isCompatibleWith(
    managerVersionCode: Int = BuildConfig.VERSION_CODE,
    ksudStatus: InstalledKsudStatus,
): Boolean = checkManagerPluginCompatibility(plugin, managerVersionCode, ksudStatus).isCompatible

internal fun resolveCompatiblePluginIds(
    installed: Iterable<InstalledManagerPlugin>,
    managerVersionCode: Int = BuildConfig.VERSION_CODE,
    ksudStatus: InstalledKsudStatus,
): Set<String> = installed
    .asSequence()
    .filter { it.isCompatibleWith(managerVersionCode, ksudStatus) }
    .mapTo(linkedSetOf()) { it.plugin.id }

internal enum class PluginRemovalResult {
    Removed, NotInstalled, RequiresStealthDisabled, RemoteManagementStopFailed, PathmaskStopFailed,
}

internal suspend fun stopPathmaskPluginForRemoval(
    readStatus: suspend () -> HiddenPathConfigReadResult,
    disableAutoLoad: suspend () -> ToolCommandResult,
    unload: suspend () -> ToolCommandResult,
): Boolean {
    val before = readStatus().config ?: return false
    if (before.hasPendingCandidate) return false
    if (!before.loaded && before.targetPaths.isEmpty()) return true
    if (!before.loaded && !before.autoLoadEnabled) return true
    if (before.autoLoadEnabled && !disableAutoLoad().success) return false
    if (!unload().success) return false
    val after = readStatus().config ?: return false
    return !after.autoLoadEnabled && !after.loaded && !after.hasPendingCandidate
}

internal suspend fun removeManagerPlugin(
    pluginId: String,
    stealthEnabled: Boolean,
    stopRemoteManagement: () -> Boolean,
    stopPathmask: suspend () -> Boolean,
    removeRecord: () -> Boolean,
): PluginRemovalResult {
    val remoteManagement = pluginId == ManagerPlugin.RemoteManagementSuite.id
    if (remoteManagement && stealthEnabled) return PluginRemovalResult.RequiresStealthDisabled
    if (remoteManagement && !stopRemoteManagement()) return PluginRemovalResult.RemoteManagementStopFailed
    if (pluginId == ManagerPlugin.PathmaskLkm.id && !stopPathmask()) {
        return PluginRemovalResult.PathmaskStopFailed
    }
    return if (removeRecord()) PluginRemovalResult.Removed else PluginRemovalResult.NotInstalled
}

internal fun parseManagerPluginCatalog(json: String): ManagerPluginCatalog {
    require(json.toByteArray(Charsets.UTF_8).size <= MAX_CATALOG_BYTES) { "Plugin catalog is too large" }
    val root = JSONObject(json)
    require(root.optString("schema") == PLUGIN_CATALOG_SCHEMA) { "Unsupported plugin catalog" }
    require(root.optInt("version") == PLUGIN_SCHEMA_VERSION) { "Unsupported plugin catalog version" }
    val items = root.optJSONArray("plugins") ?: error("Plugin catalog has no plugins")
    require(items.length() in (ManagerPlugin.entries.size - 1)..ManagerPlugin.entries.size) {
        "Plugin catalog size is invalid"
    }
    val plugins = buildList {
        for (index in 0 until items.length()) {
            add(parsePlugin(items.optJSONObject(index) ?: error("Plugin catalog entry is invalid"), requireDownload = true))
        }
    }
    val pluginIds = plugins.mapTo(linkedSetOf()) { it.id }
    val expectedIds = ManagerPlugin.entries.mapTo(linkedSetOf()) { it.id }
    require(pluginIds.size == plugins.size &&
        (pluginIds == expectedIds || pluginIds == expectedIds - ManagerPlugin.PathmaskLkm.id)) {
        "Plugin catalog identifiers do not match the Manager"
    }
    return ManagerPluginCatalog(root.optLong("generatedAt").coerceAtLeast(0L), plugins)
}

private fun parsePlugin(item: JSONObject, requireDownload: Boolean): ManagerPluginPackage {
    val instructions = item.optJSONArray("instructions") ?: JSONArray()
    val parsedInstructions = buildList {
        for (index in 0 until instructions.length()) {
            val value = instructions.optString(index).trim().take(MAX_INSTRUCTION_LENGTH)
            require(value.isNotEmpty()) { "Plugin instruction is invalid" }
            add(value)
        }
    }
    val slots = item.optJSONArray("slots") ?: JSONArray()
    val parsedSlots = buildSet {
        for (index in 0 until slots.length()) {
            add(PluginSlot.parse(slots.optString(index)) ?: error("Plugin slot is invalid"))
        }
    }
    return ManagerPluginPackage(
        id = item.optString("id"),
        version = item.optInt("version"),
        name = item.optString("name").trim().take(80),
        summary = item.optString("summary").trim().take(240),
        description = item.optString("description").trim().take(MAX_DESCRIPTION_LENGTH),
        instructions = parsedInstructions,
        slots = parsedSlots,
        minManagerVersionCode = item.optInt("minManagerVersionCode"),
        minKsudVersionCode = item.optInt("minKsudVersionCode"),
        downloadUrl = item.optString("downloadUrl"),
        sha256 = item.optString("sha256").lowercase(Locale.ROOT),
        sizeBytes = item.optLong("sizeBytes"),
    ).also { validatePlugin(it, requireDownload) }
}

private fun pluginJson(plugin: ManagerPluginPackage): JSONObject = JSONObject()
    .put("schema", PLUGIN_PACKAGE_SCHEMA)
    .put("version", plugin.version)
    .put("id", plugin.id)
    .put("name", plugin.name)
    .put("summary", plugin.summary)
    .put("description", plugin.description)
    .put("instructions", JSONArray(plugin.instructions))
    .put("slots", JSONArray(plugin.slots.map { it.id }.sorted()))
    .put("minManagerVersionCode", plugin.minManagerVersionCode)
    .put("minKsudVersionCode", plugin.minKsudVersionCode)
    .put("downloadUrl", plugin.downloadUrl)
    .put("sha256", plugin.sha256)
    .put("sizeBytes", plugin.sizeBytes)

private fun validatePlugin(plugin: ManagerPluginPackage, requireDownload: Boolean) {
    require(PLUGIN_ID_PATTERN.matches(plugin.id)) { "Plugin id is invalid" }
    val definition = requireNotNull(ManagerPlugin.fromId(plugin.id)) { "Plugin is not allowlisted" }
    require(plugin.slots == definition.slots) { "Plugin slots are invalid" }
    require(plugin.version in 1..10_000) { "Plugin version is invalid" }
    require(plugin.name.isNotBlank() && plugin.summary.isNotBlank() && plugin.description.isNotBlank()) {
        "Plugin text is invalid"
    }
    require(plugin.instructions.size in 1..MAX_INSTRUCTION_COUNT) { "Plugin instructions are invalid" }
    require(plugin.minManagerVersionCode in 1..Int.MAX_VALUE && plugin.minKsudVersionCode in 1..Int.MAX_VALUE) {
        "Plugin compatibility is invalid"
    }
    if (requireDownload) {
        require(validatePluginUrl(plugin.downloadUrl) == plugin.downloadUrl) { "Plugin URL is invalid" }
        require(HASH_PATTERN.matches(plugin.sha256)) { "Plugin hash is invalid" }
        require(plugin.sizeBytes in 1..MAX_PACKAGE_BYTES) { "Plugin size is invalid" }
    }
}

private fun validatePluginUrl(raw: String): String {
    val uri = URI(raw)
    require(uri.scheme.equals("https", ignoreCase = true) && uri.userInfo == null && uri.fragment == null) {
        "Plugin URL must use HTTPS"
    }
    require(uri.host != null && uri.host.lowercase(Locale.ROOT) in ALLOWED_GITHUB_HOSTS) {
        "Plugin URL host is not allowed"
    }
    return raw
}

internal fun resolvePluginDownloadUrls(raw: String, route: PluginDownloadRoute): List<String> {
    val original = validatePluginUrl(raw)
    val directUrls = listOfNotNull(migrateLegacyPluginStoreUrl(original), original).distinct()
    return when (route) {
        PluginDownloadRoute.Direct -> directUrls
        PluginDownloadRoute.Accelerator -> directUrls.flatMap { url ->
            listOf("https://$ACCELERATOR_HOST/$url", url)
        }
    }
}

private fun migrateLegacyPluginStoreUrl(raw: String): String? {
    val legacyPrefix = "$LEGACY_PLUGIN_STORE_BASE_URL/"
    if (!raw.startsWith(legacyPrefix)) return null
    return "$ACTIVE_PLUGIN_STORE_BASE_URL/${raw.removePrefix(legacyPrefix)}"
}

internal fun resolvePluginCatalogUrls(
    raw: String,
    route: PluginDownloadRoute,
): List<Pair<String, String>> = resolvePluginDownloadUrls(raw, route).map { catalogUrl ->
    catalogUrl to siblingPluginSignatureUrl(catalogUrl)
}

private fun siblingPluginSignatureUrl(catalogUrl: String): String {
    val queryStart = catalogUrl.indexOf('?').takeIf { it >= 0 } ?: catalogUrl.length
    val base = catalogUrl.substring(0, queryStart)
    val query = catalogUrl.substring(queryStart)
    val slash = base.lastIndexOf('/')
    require(slash >= 0 && slash < base.lastIndex) { "Plugin catalog URL has no filename" }
    val filename = base.substring(slash + 1)
    require(filename.endsWith(".json", ignoreCase = true)) {
        "Plugin catalog URL must point to JSON"
    }
    return base.substring(0, slash + 1) + filename.dropLast(".json".length) + ".sig" + query
}

private fun isAllowedPluginHost(host: String): Boolean {
    val normalized = host.lowercase(Locale.ROOT)
    return normalized in ALLOWED_GITHUB_HOSTS || normalized == ACCELERATOR_HOST
}

internal fun verifyManagerPluginCatalogSignature(
    catalog: ByteArray,
    signatureText: ByteArray,
    publicKeyBase64: String = CATALOG_PUBLIC_KEY_B64,
) {
    val signatureBytes = Base64.getMimeDecoder().decode(signatureText.toString(Charsets.UTF_8).trim())
    require(signatureBytes.size == 64) { "Plugin catalog signature is invalid" }
    val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
    val payloads = listOfNotNull(catalog, catalog.withCrlfLineEndings())
    require(verifyCatalogEd25519Signature(payloads, signatureBytes, keyBytes)) {
        "Plugin catalog signature verification failed"
    }
}

private fun ByteArray.withCrlfLineEndings(): ByteArray? {
    var loneLfCount = 0
    for (index in indices) {
        if (this[index] == '\n'.code.toByte() && (index == 0 || this[index - 1] != '\r'.code.toByte())) {
            loneLfCount++
        }
    }
    if (loneLfCount == 0) return null

    val normalized = ByteArray(size + loneLfCount)
    var target = 0
    for (index in indices) {
        val value = this[index]
        if (value == '\n'.code.toByte() && (index == 0 || this[index - 1] != '\r'.code.toByte())) {
            normalized[target++] = '\r'.code.toByte()
        }
        normalized[target++] = value
    }
    return normalized
}

private fun verifyBundledCatalogSignature(catalog: ByteArray, signature: ByteArray) {
    try {
        verifyManagerPluginCatalogSignature(catalog, signature)
    } catch (_: NoSuchAlgorithmException) {
        // Bundled assets are protected by the signed APK. Some vendor
        // images omit the Ed25519 provider, so keep the local catalog
        // usable without weakening verification for downloaded catalogs.
    }
}

private fun java.io.InputStream.readLimited(
    maximumBytes: Long,
    onRead: ((Long) -> Unit)? = null,
): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= maximumBytes) { "Plugin response is too large" }
        output.write(buffer, 0, count)
        onRead?.invoke(total)
    }
    return output.toByteArray()
}

private fun ByteArray.toHexString(): String = joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

private fun safePluginMessage(error: Throwable): String = error.message
    ?.replace(Regex("[\\r\\n]+"), " ")
    ?.take(180)
    ?.ifBlank { "Plugin catalog unavailable" }
    ?: "Plugin catalog unavailable"
