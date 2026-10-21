package com.instatrace

import java.util.Locale
import java.util.zip.ZipInputStream

data class ParsedSnapshot(val followers: Set<String>, val following: Set<String>)
data class ChangeSet(val lost: Set<String>, val gained: Set<String>, val notFollowingBack: Set<String>)

/** Parses the Followers and Following files from an Instagram Accounts Center ZIP export. */
object InstagramExportParser {
    private val followerFile = Regex("(?:^|/)followers(?:_\\d+)?\\.(?:json|html)$", RegexOption.IGNORE_CASE)
    private val followingFile = Regex("(?:^|/)following\\.(?:json|html)$", RegexOption.IGNORE_CASE)
    private val jsonValue = Regex(""""value"\s*:\s*"([^"]+)"""")
    private val jsonInstagramHref = Regex(""""href"\s*:\s*"https?://(?:www\.)?instagram\.com/([^"/?#]+)""", RegexOption.IGNORE_CASE)
    private val htmlInstagramHref = Regex("""href=["']https?://(?:www\.)?instagram\.com/([^/"'?]+)""", RegexOption.IGNORE_CASE)
    private val anyInstagramPath = Regex("""instagram\.com/([^/"'?< ]+)""", RegexOption.IGNORE_CASE)

    fun parse(zip: ZipInputStream): ParsedSnapshot {
        val followers = linkedSetOf<String>()
        val following = linkedSetOf<String>()
        var sawFollowers = false
        var sawFollowing = false

        while (true) {
            val entry = zip.nextEntry ?: break
            if (!entry.isDirectory) {
                val path = entry.name.replace('\\', '/')
                val isFollower = followerFile.containsMatchIn(path)
                val isFollowing = followingFile.containsMatchIn(path)
                if (isFollower || isFollowing) {
                    val content = zip.readBytes().toString(Charsets.UTF_8)
                    val destination = if (isFollower) followers else following
                    if (isFollower) sawFollowers = true else sawFollowing = true
                    destination.addAll(extractUsernames(content, path.endsWith(".json", ignoreCase = true)))
                }
            }
            zip.closeEntry()
        }

        if (!sawFollowers || !sawFollowing) {
            throw IllegalArgumentException("Followers/following files not found. Request both Followers and Following in Instagram's export options.")
        }
        return ParsedSnapshot(followers, following)
    }

    internal fun extractUsernames(content: String, isJson: Boolean): Set<String> {
        val primary = linkedSetOf<String>()
        if (isJson) {
            jsonValue.findAll(content).forEach { addIfUsername(primary, it.groupValues[1]) }
            if (primary.isEmpty()) {
                jsonInstagramHref.findAll(content).forEach { addIfUsername(primary, it.groupValues[1]) }
            }
        } else {
            htmlInstagramHref.findAll(content).forEach { addIfUsername(primary, it.groupValues[1]) }
            if (primary.isEmpty()) {
                anyInstagramPath.findAll(content).forEach { addIfUsername(primary, it.groupValues[1]) }
            }
        }
        return primary
    }

    private fun addIfUsername(destination: MutableSet<String>, raw: String) {
        val username = raw.trim().removePrefix("@").trimEnd('/').lowercase(Locale.ROOT)
        if (username.isNotBlank() && username.matches(Regex("[a-z0-9._]+")) && username !in setOf("p", "reel", "reels", "stories", "explore", "accounts")) {
            destination.add(username)
        }
    }
}
