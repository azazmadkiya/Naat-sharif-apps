package naatsharif.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NaatDao {

    @Query("SELECT * FROM naat_items WHERE isDeleted = 0 AND status = 'APPROVED' ORDER BY orderIndex ASC, titleHindi ASC")
    fun getAllApprovedNaats(): Flow<List<NaatEntity>>

    @Query("SELECT * FROM naat_items WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY titleHindi ASC")
    fun getFavoriteNaats(): Flow<List<NaatEntity>>

    @Query("SELECT * FROM naat_items WHERE isDeleted = 0 AND status = 'APPROVED' AND category = :category ORDER BY orderIndex ASC")
    fun getNaatsByCategory(category: String): Flow<List<NaatEntity>>

    @Query("SELECT * FROM naat_items WHERE isDeleted = 0 AND status = 'PENDING_APPROVAL' ORDER BY createdAt DESC")
    fun getPendingApprovalNaats(): Flow<List<NaatEntity>>

    @Query("SELECT * FROM naat_items WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedNaats(): Flow<List<NaatEntity>>

    @Query("SELECT * FROM naat_items WHERE isDeleted = 1")
    suspend fun getDeletedNaatsSync(): List<NaatEntity>

    @Query("SELECT * FROM naat_items WHERE id = :id LIMIT 1")
    suspend fun getNaatById(id: String): NaatEntity?

    @Query("SELECT * FROM naat_items WHERE id = :id LIMIT 1")
    fun observeNaatById(id: String): Flow<NaatEntity?>

    @Query("SELECT COUNT(*) FROM naat_items WHERE isDeleted = 0 AND status = 'APPROVED'")
    fun getApprovedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM naat_items WHERE isDeleted = 0 AND status = 'PENDING_APPROVAL'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM naat_items WHERE isDeleted = 1")
    fun getDeletedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(naat: NaatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(naats: List<NaatEntity>)

    @Update
    suspend fun update(naat: NaatEntity)

    @Query("UPDATE naat_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE naat_items SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE naat_items SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE naat_items SET isDeleted = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreFromTrash(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM naat_items WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM naat_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM naat_items WHERE titleHindi LIKE '%कोई तो है जो निज़ामे हस्ती%' OR titleHindi LIKE '%फासलों को तकल्लुफ%' OR titleHindi LIKE '%तू कुजा मन कुजा%' OR titleHindi LIKE '%भर दो झोली%' OR titleHindi LIKE '%करम मांगता हूँ%' OR titleHindi LIKE '%अल्लाहु अल्लाहु%' OR titleHindi LIKE '%मुस्तफा जाने रहमत%' OR titleHindi LIKE '%चमक तुझसे पाते%' OR titleHindi LIKE '%बगदाद वाले दूल्हा%' OR titleHindi LIKE '%ख्वाजा मेरे ख्वाजा%' OR titleHindi LIKE '%अली मौला अली मौला%' OR titleHindi LIKE '%हुसैन इब्ने अली%' OR titleHindi LIKE '%या नबी सलाम अलैका%'")
    suspend fun deleteDefaultTitles()

    @Query("DELETE FROM naat_items WHERE titleEnglish LIKE '%Woh Wohi Khuda Hai%' OR titleEnglish LIKE '%Faslon Ko Takalluf%' OR titleEnglish LIKE '%Tu Kuja Man Kuja%' OR titleEnglish LIKE '%Bhar Do Jholi%' OR titleEnglish LIKE '%Karam Mangta Hoon%' OR titleEnglish LIKE '%Allahu Allahu%' OR titleEnglish LIKE '%Mustafa Jane Rehmat%' OR titleEnglish LIKE '%Chamak Tujhse%' OR titleEnglish LIKE '%Baghdad Wale Dulha%' OR titleEnglish LIKE '%Khwaja Mere Khwaja%' OR titleEnglish LIKE '%Ali Maula%' OR titleEnglish LIKE '%Hussain Ibne Ali%' OR titleEnglish LIKE '%Ya Nabi Salam Alaika%'")
    suspend fun deleteDefaultEnglishTitles()

    @Query("DELETE FROM naat_items WHERE isDeleted = 1")
    suspend fun emptyTrashBin()

    @Query("SELECT COUNT(*) FROM naat_items")
    suspend fun getTotalCountSync(): Int

    @Query("SELECT * FROM naat_items")
    suspend fun getAllNaatsSync(): List<NaatEntity>

    @Query("DELETE FROM naat_items WHERE addedBy IN ('Admin', 'System', 'Default', 'Sample') OR id LIKE 'naat_%' OR id LIKE 'hamd_%' OR id LIKE 'raza_%' OR id LIKE 'gouse_%' OR id LIKE 'gareeb_%' OR id LIKE 'manqabat_%' OR id LIKE 'panjtan_%' OR id LIKE 'salami_%' OR id LIKE 'default_%'")
    suspend fun deleteAllSystemAndDefaultNaats()

    @Query("DELETE FROM naat_items")
    suspend fun clearAllNaats()
}
