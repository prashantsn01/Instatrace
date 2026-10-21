package com.instatrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedInputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = InstaTraceDatabase.get(this)
        setContent { InstaTraceApp(db) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstaTraceApp(db: InstaTraceDatabase) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snapshots by db.snapshotDao().observeAll().collectAsState(initial = emptyList())
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true; error = null; notice = null
            scope.launch {
                try {
                    val parsed = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            ZipInputStream(BufferedInputStream(stream)).use(InstagramExportParser::parse)
                        } ?: throw IllegalArgumentException("Could not open that file.")
                    }
                    if (parsed.followers.isEmpty() && parsed.following.isEmpty()) throw IllegalArgumentException("The export was parsed but no usernames were found. Check that Followers and Following were included.")
                    val entity = SnapshotEntity(
                        importedAt = System.currentTimeMillis(),
                        followersJson = JSONArray(parsed.followers.sorted()).toString(),
                        followingJson = JSONArray(parsed.following.sorted()).toString()
                    )
                    db.snapshotDao().insert(entity)
                    notice = "Imported ${parsed.followers.size} followers and ${parsed.following.size} following."
                } catch (e: Exception) {
                    error = e.message ?: "Could not parse this export. Choose the original Instagram ZIP."
                } finally { busy = false }
            }
        }
    }
    val latest = snapshots.firstOrNull()
    val previous = snapshots.getOrNull(1)
    fun decode(s: SnapshotEntity?, followers: Boolean): Set<String> {
        if (s == null) return emptySet()
        val json = if (followers) s.followersJson else s.followingJson
        return runCatching { val a = JSONArray(json); (0 until a.length()).map { a.getString(it) }.toSet() }.getOrDefault(emptySet())
    }
    val latestFollowers = decode(latest, true)
    val latestFollowing = decode(latest, false)
    val previousFollowers = decode(previous, true)
    val changes = compareSnapshots(if (previous == null) null else previousFollowers, latestFollowers, latestFollowing)
    MaterialTheme(colorScheme = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFFB8A1FF), secondary = androidx.compose.ui.graphics.Color(0xFFFF8BCB))) {
        Scaffold(topBar = { TopAppBar(title = { Text("InstaTrace", fontWeight = FontWeight.Bold) }, actions = { Icon(Icons.Default.Security, contentDescription = "Stored locally", modifier = Modifier.padding(end = 16.dp)) }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Instagram follower history · private by design", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Followers", latest?.let { latestFollowers.size.toString() } ?: "—", Modifier.weight(1f))
                            StatCard("Following", latest?.let { latestFollowing.size.toString() } ?: "—", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Unfollowed", if (previous == null) "—" else changes.lost.size.toString(), Modifier.weight(1f))
                            StatCard("New followers", if (previous == null) "—" else changes.gained.size.toString(), Modifier.weight(1f))
                        }
                        Button(onClick = { launcher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.FileOpen, contentDescription = null); Spacer(Modifier.width(8.dp))
                            Text(if (busy) "Importing…" else "Import Instagram data ZIP")
                        }
                        Text("First import = baseline. Import another export later to compare.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (snapshots.isNotEmpty()) {
                    TabRow(selectedTabIndex = tab) {
                        listOf("Unfollowers", "New", "Not back", "History", "Limits").forEachIndexed { i, label ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, maxLines = 1) })
                        }
                    }
                    when (tab) {
                        0 -> NamesList(changes.lost, if (previous == null) "Import another snapshot to identify unfollowers." else "Not present in the latest follower list")
                        1 -> NamesList(changes.gained, if (previous == null) "Import another snapshot to see new followers." else "New since the previous snapshot")
                        2 -> NamesList(changes.notFollowingBack, "Accounts you follow that are not following you back")
                        3 -> HistoryList(snapshots, onDelete = { scope.launch { db.snapshotDao().clear(); notice = "Local history cleared." } })
                        else -> Limits()
                    }
                } else EmptyState()
            }
        }
    }
}

@Composable private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelMedium) } }
}
@Composable private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Import your official Instagram data export", fontWeight = FontWeight.SemiBold)
        Text("No Instagram password needed. Your data stays on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun NamesList(names: Set<String>, emptyMessage: String) {
    if (names.isEmpty()) Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) { items(names.sorted()) { name ->
        ListItem(leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) }, headlineContent = { Text("@$name") })
    } }
}
@Composable private fun HistoryList(snapshots: List<SnapshotEntity>, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Saved on this device", fontWeight = FontWeight.Bold)
        snapshots.forEachIndexed { index, item ->
            val time = Instant.ofEpochMilli(item.importedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
            ListItem(
                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                headlineContent = { Text("Snapshot ${snapshots.size - index}") },
                supportingContent = { Text("$time · ${JSONArray(item.followersJson).length()} followers · ${JSONArray(item.followingJson).length()} following") }
            )
        }
        OutlinedButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Clear local history") }
    }
}
@Composable private fun Limits() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Platform limitations", fontWeight = FontWeight.Bold)
        ListItem(leadingContent = { Icon(Icons.Default.VisibilityOff, contentDescription = null) }, headlineContent = { Text("Profile visitors") }, supportingContent = { Text("Instagram doesn't expose a complete list of named profile visitors.") })
        ListItem(leadingContent = { Icon(Icons.Default.Numbers, contentDescription = null) }, headlineContent = { Text("Visits per person") }, supportingContent = { Text("Per-person visit counts are not available to third-party apps.") })
        ListItem(leadingContent = { Icon(Icons.Default.Photo, contentDescription = null) }, headlineContent = { Text("What they viewed") }, supportingContent = { Text("No per-visitor browsing history is exposed.") })
        ListItem(leadingContent = { Icon(Icons.Default.Block, contentDescription = null) }, headlineContent = { Text("Blocks and exact dates") }, supportingContent = { Text("A missing profile is not proof of a block. Exact historical block dates aren't supplied.") })
        ListItem(leadingContent = { Icon(Icons.Default.Insights, contentDescription = null) }, headlineContent = { Text("Professional insights") }, supportingContent = { Text("Aggregate analytics require an eligible account and approved Meta API permissions; not connected in this build.") })
    }
}

