/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class ActionRole { Pause, Resume, Stop, Lap, Reset, AddMinute, Other }

private val PAUSE_WORDS = listOf("pause", "pausa", "pausar", "sospendi", "interrompi", "onderbreek", "रोकें", "暂停", "一時停止", "пауза")
private val RESUME_WORDS = listOf("resume", "continue", "unpause", "play", "start", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
private val STOP_WORDS = listOf("stop", "finish", "save", "detener", "parar", "arrêter", "beenden", "停止", "结束", "стоп")
private val RESET_WORDS = listOf("reset", "cancel", "clear", "delete", "dismiss", "annuler", "abbrechen", "eliminar", "borrar", "重置", "取消")
private val LAP_WORDS = listOf("lap", "split", "vuelta", "tour", "runde", "分段")

private val ADD_MINUTE_SIGN = Regex("""(?<![\d:])\+\s*0?1(:00)?\s*(m|min|mins|minute|minutes)?(?![\d:a-z])""")
private val ADD_MINUTE_WORDS = Regex("""(add|extend|more|another)\s*(an?\s*)?(1|one)\s*(m|min|mins|minute|minutes)\b""")

fun classifyAction(title: String): ActionRole {
    val t = title.lowercase()
    return when {
        ADD_MINUTE_SIGN.containsMatchIn(t) || ADD_MINUTE_WORDS.containsMatchIn(t) -> ActionRole.AddMinute
        LAP_WORDS.any { t.contains(it) } -> ActionRole.Lap
        PAUSE_WORDS.any { t.contains(it) } -> ActionRole.Pause
        RESUME_WORDS.any { t.contains(it) } -> ActionRole.Resume
        RESET_WORDS.any { t.contains(it) } -> ActionRole.Reset
        STOP_WORDS.any { t.contains(it) } -> ActionRole.Stop
        else -> ActionRole.Other
    }
}
