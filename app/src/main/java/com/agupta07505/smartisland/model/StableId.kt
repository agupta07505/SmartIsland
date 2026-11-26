/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.enums.enumEntries

interface StableId {
    val id: String
}

inline fun <reified E> enumByIdOrNull(id: String?, ignoreCase: Boolean = false): E?
    where E : Enum<E>, E : StableId =
    if (id == null) null else enumEntries<E>().firstOrNull { it.id.equals(id, ignoreCase) }

inline fun <reified E> enumById(id: String?, default: E, ignoreCase: Boolean = false): E
    where E : Enum<E>, E : StableId =
    enumByIdOrNull<E>(id, ignoreCase) ?: default
