package com.instatrace

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val importedAt: Long,
    val followersJson: String,
    val followingJson: String
)

@Dao
interface SnapshotDao {
    @Insert suspend fun insert(snapshot: SnapshotEntity): Long
    @Query("SELECT * FROM snapshots ORDER BY importedAt DESC, id DESC") fun observeAll(): Flow<List<SnapshotEntity>>
    @Query("SELECT * FROM snapshots ORDER BY importedAt DESC, id DESC LIMIT 2") suspend fun latestTwo(): List<SnapshotEntity>
    @Query("DELETE FROM snapshots") suspend fun clear()
}

@Database(entities = [SnapshotEntity::class], version = 1, exportSchema = false)
abstract class InstaTraceDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    companion object {
        @Volatile private var instance: InstaTraceDatabase? = null
        fun get(context: Context): InstaTraceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, InstaTraceDatabase::class.java, "instatrace.db").build().also { instance = it }
        }
    }
}
