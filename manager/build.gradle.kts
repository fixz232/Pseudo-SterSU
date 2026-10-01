import java.util.Properties

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val androidMinSdkVersion by extra(31)
val androidTargetSdkVersion by extra(37)
val androidCompileSdkVersion by extra(37)
val androidCompileSdkVersionMinor by extra(0)
val androidBuildToolsVersion by extra("37.0.0")
val androidCompileNdkVersion: String by extra(libs.versions.ndk.get())
val androidSourceCompatibility by extra(JavaVersion.VERSION_21)
val androidTargetCompatibility by extra(JavaVersion.VERSION_21)
val managerVersionCode by extra(getVersionCode())
val managerVersionName by extra(getVersionName())

fun runGitCommand(vararg args: String): String? = runCatching {
    val process = Runtime.getRuntime().exec(arrayOf("git", *args))
    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    val exitCode = process.waitFor()
    output.takeIf { exitCode == 0 && it.isNotEmpty() }
}.getOrNull()

fun getGitCommitCount(): Int {
    return runGitCommand("rev-list", "--count", "HEAD")?.toIntOrNull()
        ?: readFallbackVersionCode()?.let { it - 30000 }
        ?: 0
}

fun getGitDescribe(): String {
    return runGitCommand("describe", "--tags", "--always")
        ?: readFallbackVersionName()
        ?: "local"
}

fun getVersionCode(): Int {
    readFallbackVersionCode()?.let { return it }
    val commitCount = getGitCommitCount()
    return 30000 + commitCount
}

fun getVersionName(): String {
    return readFallbackVersionName() ?: getGitDescribe()
}

fun readFallbackVersionCode(): Int? {
    return readFallbackVersionProperty("versionCode")?.toIntOrNull()
}

fun readFallbackVersionName(): String? {
    return readFallbackVersionProperty("versionName")
}

fun readFallbackVersionProperty(key: String): String? {
    val file = rootProject.file("../version.properties")
    if (!file.isFile) return null

    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties.getProperty(key)?.takeIf { it.isNotBlank() }
}
