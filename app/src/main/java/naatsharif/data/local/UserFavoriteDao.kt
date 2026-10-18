package naatsharif.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFavoriteDao {

    @Query("SELECT naatId FROM user_favorites WHERE userEmail = :userEmail")
    fun getFavoriteNaatIds(userEmail: String): Flow<List<String>>

    @Query("SELECT naatId FROM user_favorites WHERE userEmail = :userEmail")
    suspend fun getFavoriteNaatIdsSync(userEmail: String): List<String>

    @Query("SELECT * FROM naat_items WHERE isDeleted = 0 AND id IN (SELECT naatId FROM user_favorites WHERE userEmail = :userEmail) ORDER BY titleHindi ASC")
    fun getFavoriteNaatsForUser(userEmail: String): Flow<List<NaatEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_favorites WHERE userEmail = :userEmail AND naatId = :naatId)")
    fun isFavorite(userEmail: String, naatId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM user_favorites WHERE userEmail = :userEmail AND naatId = :naatId)")
    suspend fun isFavoriteSync(userEmail: String, naatId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: UserFavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favs: List<UserFavoriteEntity>)

    @Query("DELETE FROM user_favorites WHERE userEmail = :userEmail AND naatId = :naatId")
    suspend fun removeFavorite(userEmail: String, naatId: String)

    @Query("DELETE FROM user_favorites WHERE userEmail = :userEmail")
    suspend fun clearUserFavorites(userEmail: String)

    @Query("SELECT COUNT(*) FROM user_favorites WHERE userEmail = :userEmail")
    fun getFavoriteCount(userEmail: String): Flow<Int>
}
