package com.instatrace

/** Compares two imported snapshots. A null previous list means there is no baseline yet. */
fun compareSnapshots(
    previousFollowers: Set<String>?,
    currentFollowers: Set<String>,
    currentFollowing: Set<String>
): ChangeSet = ChangeSet(
    lost = previousFollowers?.minus(currentFollowers) ?: emptySet(),
    gained = previousFollowers?.let { currentFollowers - it } ?: emptySet(),
    notFollowingBack = currentFollowing - currentFollowers
)
