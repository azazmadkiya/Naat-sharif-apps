package naatsharif.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user_favorites",
    primaryKeys = ["userEmail", "naatId"],
    indices = [
        Index(value = ["userEmail"]),
        Index(value = ["naatId"])
    ]
)
data class UserFavoriteEntity(
    val userEmail: String,
    val naatId: String,
    val createdAt: Long = System.currentTimeMillis()
)
