package com.instatrace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class InstagramExportParserTest {
    @Test fun parsesJsonAndMultipleFollowerParts() {
        val result = parseZip(
            "connections/followers_and_following/followers_1.json" to """[{"string_list_data":[{"href":"https://www.instagram.com/Alice/","value":"Alice","timestamp":1}]}]""",
            "connections/followers_and_following/followers_2.json" to """[{"string_list_data":[{"href":"https://www.instagram.com/bob/","value":"Bob","timestamp":2}]}]""",
            "connections/followers_and_following/following.json" to """{"relationships_following":[{"title":"Alice","string_list_data":[{"href":"https://www.instagram.com/Alice/","value":"Alice","timestamp":1}]}]}"""
        )
        assertEquals(setOf("alice", "bob"), result.followers)
        assertEquals(setOf("alice"), result.following)
    }

    @Test fun parsesHtmlExports() {
        val result = parseZip(
            "followers.html" to "<a href=\"https://www.instagram.com/Alice/\">Alice</a><a href='https://instagram.com/bob/'>Bob</a>",
            "following.html" to "<a href=\"https://www.instagram.com/Carol/\">Carol</a>"
        )
        assertEquals(setOf("alice", "bob"), result.followers)
        assertEquals(setOf("carol"), result.following)
    }

    @Test fun rejectsZipWithoutBothFiles() {
        try {
            parseZip("followers_1.json" to "[]")
            throw AssertionError("Expected missing following file to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Followers/following files not found"))
        }
    }

    @Test fun deduplicatesAndNormalizesNames() {
        val result = parseZip(
            "followers_1.json" to """[{"value":"Alice"},{"value":"alice"},{"value":"@BOB"}]""",
            "following.json" to """[{"value":"Carol"}]"""
        )
        assertEquals(setOf("alice", "bob"), result.followers)
        assertEquals(setOf("carol"), result.following)
    }

    private fun parseZip(vararg files: Pair<String, String>): ParsedSnapshot {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return ZipInputStream(ByteArrayInputStream(bytes.toByteArray())).use(InstagramExportParser::parse)
    }
}
