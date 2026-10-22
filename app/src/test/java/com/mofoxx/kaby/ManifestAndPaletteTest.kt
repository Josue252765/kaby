package com.mofoxx.kaby

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

class ManifestAndPaletteTest {
    @Test
    fun testRespaldo_reglasAndroid() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val legacy = File("src/main/res/xml/backup_rules.xml").readText()
        val modern = File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue(legacy.contains("<exclude"))
        assertTrue(modern.contains("<cloud-backup"))
        assertTrue(modern.contains("<device-transfer"))
    }

    @Test
    fun testRespaldo_intentoDiarioAlAbrirYAlPasarSegundoPlano() {
        val application = File("src/main/java/com/mofoxx/kaby/KabyApplication.kt").readText()
        val activity = File("src/main/java/com/mofoxx/kaby/MainActivity.kt").readText()
        val scheduler = File("src/main/java/com/mofoxx/kaby/backup/BackupScheduler.kt").readText()
        assertTrue(application.contains("BackupScheduler.schedule(this)"))
        assertTrue(activity.contains("BackupScheduler.scheduleAfterBackground(this)"))
        assertTrue(scheduler.contains("ExistingPeriodicWorkPolicy.KEEP"))
        assertTrue(scheduler.contains("BACKGROUND_NAME"))
    }

    @Test
    fun testManifest_iconoUsaLaPaletaKaby() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val icon = File("src/main/res/drawable/ic_launcher.xml").readText()
        val ink = paletteColor("Ink")
        val paper = paletteColor("Paper")
        assertTrue(manifest.contains("android:icon=\"@drawable/ic_launcher\""))
        assertTrue(icon.contains("#$ink", ignoreCase = true))
        assertTrue(icon.contains("#$paper", ignoreCase = true))
    }

    @Test
    fun testContraste_paletaAA() {
        val canvas = paletteColor("CanvasStone").toInt(16)
        val surface = paletteColor("SurfaceColor").toInt(16)
        val ink = paletteColor("Ink").toInt(16)
        val muted = paletteColor("Muted").toInt(16)
        val teal = paletteColor("Teal").toInt(16)
        val terracotta = paletteColor("Terracotta").toInt(16)
        assertTrue(contrast(ink, canvas) >= 4.5)
        assertTrue(contrast(muted, surface) >= 4.5)
        assertTrue(contrast(teal, surface) >= 4.5)
        assertTrue(contrast(terracotta, surface) >= 4.5)
    }

    @Test
    fun testTemaUsaTintaPiedraYTonoPrincipalAccesible() {
        val source = File("src/main/java/com/mofoxx/kaby/ui/KabyApp.kt").readText()
        val theme = source.substringAfter("fun KabyTheme").substringBefore("private enum class Page")

        assertTrue(theme.contains("colorScheme = lightColorScheme("))
        assertTrue(theme.contains("primary = Teal"))
        assertTrue(theme.contains("onPrimary = Color.White"))
        assertTrue(theme.contains("background = CanvasStone"))
        assertTrue(theme.contains("surface = SurfaceColor"))
        assertTrue(theme.contains("onSurfaceVariant = Muted"))
    }

    private fun paletteColor(name: String): String {
        val source = File("src/main/java/com/mofoxx/kaby/ui/KabyApp.kt").readText()
        val match = Regex("private val $name = Color\\(0xFF([0-9A-Fa-f]{6})\\)").find(source)
            ?: error("No se encontró el color $name en la paleta Compose")
        return match.groupValues[1]
    }

    private fun contrast(first: Int, second: Int): Double {
        val a = luminance(first)
        val b = luminance(second)
        val high = maxOf(a, b)
        val low = minOf(a, b)
        return (high + 0.05) / (low + 0.05)
    }

    private fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((color shr shift) and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
