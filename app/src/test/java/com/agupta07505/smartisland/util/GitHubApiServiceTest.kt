/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubApiServiceTest {

    @Test
    fun testIsNewerVersion_newerMajor_returnsTrue() {
        assertTrue(GitHubApiService.isNewerVersion("v6.0.0", "5.0.0"))
        assertTrue(GitHubApiService.isNewerVersion("6.0", "5.2.1"))
    }

    @Test
    fun testIsNewerVersion_newerMinor_returnsTrue() {
        assertTrue(GitHubApiService.isNewerVersion("v5.1.0", "5.0.0"))
        assertTrue(GitHubApiService.isNewerVersion("5.2", "5.1.9"))
    }

    @Test
    fun testIsNewerVersion_newerPatch_returnsTrue() {
        assertTrue(GitHubApiService.isNewerVersion("v5.0.1", "5.0.0"))
        assertTrue(GitHubApiService.isNewerVersion("5.0.2", "5.0.1"))
    }

    @Test
    fun testIsNewerVersion_sameVersion_returnsFalse() {
        assertFalse(GitHubApiService.isNewerVersion("v5.0.0", "5.0.0"))
        assertFalse(GitHubApiService.isNewerVersion("5.0", "5.0"))
    }

    @Test
    fun testIsNewerVersion_olderVersion_returnsFalse() {
        assertFalse(GitHubApiService.isNewerVersion("v4.9.9", "5.0.0"))
        assertFalse(GitHubApiService.isNewerVersion("5.0.0", "5.0.1"))
        assertFalse(GitHubApiService.isNewerVersion("4.0", "5.0"))
    }

    @Test
    fun testIsNewerVersion_withSuffix_handlesCleanly() {
        assertTrue(GitHubApiService.isNewerVersion("v5.1.0-beta", "5.0.0"))
        assertFalse(GitHubApiService.isNewerVersion("v5.0.0-rc1", "5.0.0"))
    }
}
