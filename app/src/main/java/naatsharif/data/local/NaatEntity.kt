package naatsharif.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "naat_items")
data class NaatEntity(
    @PrimaryKey
    val id: String,
    val titleHindi: String,
    val titleGujarati: String,
    val titleEnglish: String,
    val lyricsHindi: String,
    val lyricsGujarati: String,
    val lyricsEnglish: String,
    val category: String, // e.g., "हम्द शरीफ", "नात शरीफ", "मनक़बत शरीफ", "पंजतन पाक", "गौसे आज़म", "गरीब नवाज", "कलामे रज़ा", "सलामी"
    val poet: String = "Traditional / Kalaam",
    val audioUrl: String = "",
    val orderIndex: Int = 0,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val addedBy: String = "Azazmadkiya",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "APPROVED" // "APPROVED", "PENDING_APPROVAL"
) {
    @get:Ignore
    val title: String
        get() = titleHindi.ifBlank { titleEnglish.ifBlank { titleGujarati } }

    @get:Ignore
    val artist: String
        get() = poet

    @get:Ignore
    val lyrics: String
        get() = lyricsHindi.ifBlank { lyricsEnglish.ifBlank { lyricsGujarati } }

    @get:Ignore
    val isApproved: Boolean
        get() = status == "APPROVED"
}
