/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlinx.coroutines.CancellationException

/*
 * How failures are handled here, 3 kinds:
 *  - expected: a read that may legitimately fail (a field of a foreign notification), the caller has a default and
 *    nothing is logged -> [quietly]
 *  - unexpected: something that should work, log it at error level and carry on -> [runCatchingLogged] or, in
 *    suspend code, [runSuspendCatchingLogged]
 *  - never swallowed: a CancellationException, the suspend helpers rethrow it
 *
 * Plain runCatching is left for the places that really use the Result (isSuccess, onFailure, getOrElse) and for
 * the logger itself, which can't report its own failures. Everything else goes through the helpers below.
 * Caveat: [quietly] and a bare runCatching also swallow a CancellationException, so inside suspend code that has to
 * stop on cancel use [runSuspendCatching] / [runSuspendCatchingLogged].
 */

/** Expected failure: the value, or null when [block] throws. Same reach as runCatching, so it also catches Errors. */
inline fun <T> quietly(block: () -> T): T? = try {
    block()
} catch (ignored: Throwable) {
    null
}

/** Expected failure with a default instead of null. */
inline fun <T> quietly(default: T, block: () -> T): T = try {
    block()
} catch (ignored: Throwable) {
    default
}

inline fun <T> runCatchingLogged(tag: String, message: String = "Operation failed", block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        runCatching { AppLogRecorder.e(tag, message, e) }
        null
    }
}

suspend inline fun <T> runSuspendCatchingLogged(
    tag: String,
    message: String = "Operation failed",
    crossinline block: suspend () -> T
): T? {
    return try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        runCatching { AppLogRecorder.e(tag, message, error) }
        null
    }
}

/** [runCatching] for suspend code, a cancellation is rethrown and never held in the [Result]. */
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
