package naatsharif.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import naatsharif.data.local.DefaultNaatData
import naatsharif.data.local.NaatDao
import naatsharif.data.local.NaatDatabase
import naatsharif.data.local.NaatEntity
import naatsharif.data.local.UserFavoriteDao
import naatsharif.data.local.UserFavoriteEntity
import naatsharif.data.preferences.UserPreferences
import java.util.UUID

class NaatRepository(context: Context) {
    private val database = NaatDatabase.getDatabase(context)
    private val naatDao: NaatDao = database.naatDao()
    private val userFavoriteDao: UserFavoriteDao = database.userFavoriteDao()
    private val preferences: UserPreferences = UserPreferences(context)
    private var firestore: FirebaseFirestore? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var realtimeListener: ListenerRegistration? = null
    private var userFavoritesListener: ListenerRegistration? = null

    val currentUserEmailFlow: Flow<String> = preferences.currentUserEmailFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val userFavoriteIds: Flow<Set<String>> = currentUserEmailFlow.flatMapLatest { email ->
        userFavoriteDao.getFavoriteNaatIds(email).map { it.toSet() }
    }

    val approvedNaats: Flow<List<NaatEntity>> = combine(
        naatDao.getAllApprovedNaats(),
        userFavoriteIds
    ) { naats, favIds ->
        naats.map { naat ->
            naat.copy(isFavorite = favIds.contains(naat.id))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteNaats: Flow<List<NaatEntity>> = currentUserEmailFlow.flatMapLatest { email ->
        userFavoriteDao.getFavoriteNaatsForUser(email).map { list ->
            list.map { it.copy(isFavorite = true) }
        }
    }

    val pendingApprovalNaats: Flow<List<NaatEntity>> = naatDao.getPendingApprovalNaats()
    val deletedNaats: Flow<List<NaatEntity>> = naatDao.getDeletedNaats()

    val approvedCount: Flow<Int> = naatDao.getApprovedCount()
    val pendingCount: Flow<Int> = naatDao.getPendingCount()
    val deletedCount: Flow<Int> = naatDao.getDeletedCount()

    init {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            if (app != null) {
                firestore = FirebaseFirestore.getInstance(app)
                Log.i("NaatRepository", "Firebase Firestore connected successfully")
                startRealtimeSync()
            }
        } catch (e: Throwable) {
            Log.i("NaatRepository", "Running in offline Room mode (Firebase init note): ${e.message}")
        }

        // Listen to active user changes to dynamically sync remote favorites from Firestore
        repositoryScope.launch {
            currentUserEmailFlow.collect { email ->
                listenToUserFavorites(email)
            }
        }

        // Remove all default Naat Sharif as requested
        repositoryScope.launch {
            purgeAllDefaultNaats()
        }
    }

    /**
     * Real-time listener for Firestore collection 'naats'.
     * Whenever Admin adds, edits, approves, or deletes a Naat in Firestore,
     * it immediately updates the local Room DB and reflects in users' UI.
     */
    fun startRealtimeSync() {
        val fs = firestore ?: return
        try {
            realtimeListener?.remove()
            realtimeListener = fs.collection("naats").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("NaatRepository", "Firestore realtime sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    repositoryScope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                if (permanentlyDeletedIds.contains(id)) continue
                                val lastUpdate = recentlyUpdatedIds[id]
                                if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < 15000) continue

                                val titleHindi = doc.getString("titleHindi") ?: ""
                                val titleGujarati = doc.getString("titleGujarati") ?: ""
                                val titleEnglish = doc.getString("titleEnglish") ?: ""

                                // Filter out any default Naats and delete them from Firestore and Room DB
                                if (DefaultNaatData.isDefaultNaat(id, titleHindi, titleEnglish)) {
                                    naatDao.deletePermanently(id)
                                    deleteFromFirestore(id)
                                    continue
                                }

                                val isDeleted = doc.getBoolean("isDeleted") ?: false
                                val status = doc.getString("status") ?: "APPROVED"
                                val lyricsHindi = doc.getString("lyricsHindi") ?: ""
                                val lyricsGujarati = doc.getString("lyricsGujarati") ?: ""
                                val lyricsEnglish = doc.getString("lyricsEnglish") ?: ""
                                val category = doc.getString("category") ?: "नात शरीफ"
                                val poet = doc.getString("poet") ?: "Traditional / Kalaam"
                                val audioUrl = doc.getString("audioUrl") ?: ""
                                val orderIndex = (doc.getLong("orderIndex") ?: 0L).toInt()
                                val addedBy = doc.getString("addedBy") ?: "Admin"
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                                val existing = naatDao.getNaatById(id)
                                val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
                                if (existing != null && existing.updatedAt > remoteUpdatedAt && (System.currentTimeMillis() - existing.updatedAt < 60000)) {
                                    continue
                                }

                                val entity = NaatEntity(
                                    id = id,
                                    titleHindi = titleHindi,
                                    titleGujarati = titleGujarati,
                                    titleEnglish = titleEnglish,
                                    lyricsHindi = lyricsHindi,
                                    lyricsGujarati = lyricsGujarati,
                                    lyricsEnglish = lyricsEnglish,
                                    category = category,
                                    poet = poet,
                                    audioUrl = audioUrl,
                                    orderIndex = orderIndex,
                                    isFavorite = false, // Individual favorite state is isolated per user
                                    isDeleted = isDeleted,
                                    addedBy = addedBy,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt,
                                    status = status
                                )
                                naatDao.insertOrUpdate(entity)
                            } catch (e: Exception) {
                                Log.e("NaatRepository", "Error syncing naat doc: ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w("NaatRepository", "Realtime sync listener setup error: ${e.message}")
        }
    }

    private fun listenToUserFavorites(email: String) {
        userFavoritesListener?.remove()
        userFavoritesListener = null
        if (email == "guest" || email.isBlank()) return
        val fs = firestore ?: return
        try {
            val cleanEmail = email.trim().lowercase()
            userFavoritesListener = fs.collection("users").document(cleanEmail)
                .collection("favorites").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("NaatRepository", "User favorites realtime sync error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        repositoryScope.launch(Dispatchers.IO) {
                            val remoteFavIds = snapshot.documents.mapNotNull { doc ->
                                doc.getString("naatId") ?: doc.id.ifBlank { null }
                            }
                            for (favId in remoteFavIds) {
                                userFavoriteDao.addFavorite(UserFavoriteEntity(userEmail = cleanEmail, naatId = favId))
                            }
                        }
                    }
                }
        } catch (e: Throwable) {
            Log.w("NaatRepository", "Failed setting up user favorites listener: ${e.message}")
        }
    }

    private fun syncUserFavoriteToFirestore(email: String, naatId: String) {
        if (email == "guest" || email.isBlank()) return
        val fs = firestore ?: return
        try {
            val cleanEmail = email.trim().lowercase()
            fs.collection("users").document(cleanEmail)
                .collection("favorites").document(naatId)
                .set(
                    mapOf(
                        "naatId" to naatId,
                        "userEmail" to cleanEmail,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).addOnSuccessListener {
                    Log.i("NaatRepository", "User favorite synced: $naatId for $cleanEmail")
                }.addOnFailureListener { e ->
                    Log.w("NaatRepository", "User favorite sync failed: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.w("NaatRepository", "Firestore favorite sync ignored: ${e.message}")
        }
    }

    private fun deleteUserFavoriteFromFirestore(email: String, naatId: String) {
        if (email == "guest" || email.isBlank()) return
        val fs = firestore ?: return
        try {
            val cleanEmail = email.trim().lowercase()
            fs.collection("users").document(cleanEmail)
                .collection("favorites").document(naatId)
                .delete()
                .addOnSuccessListener {
                    Log.i("NaatRepository", "User favorite deleted from cloud: $naatId for $cleanEmail")
                }.addOnFailureListener { e ->
                    Log.w("NaatRepository", "User favorite delete failed: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.w("NaatRepository", "Firestore favorite delete ignored: ${e.message}")
        }
    }

    suspend fun purgeAllDefaultNaats() = withContext(Dispatchers.IO) {
        try {
            naatDao.deleteAllSystemAndDefaultNaats()
            naatDao.deleteByIds(DefaultNaatData.defaultIdsToRemove)
            for (defaultId in DefaultNaatData.defaultIdsToRemove) {
                naatDao.deletePermanently(defaultId)
                deleteFromFirestore(defaultId)
            }
            naatDao.deleteDefaultTitles()
            naatDao.deleteDefaultEnglishTitles()

            val localItems = naatDao.getAllNaatsSync()
            for (item in localItems) {
                if (DefaultNaatData.isDefaultNaat(item.id, item.titleHindi, item.titleEnglish) &&
                    item.addedBy in listOf("Admin", "System", "Default", "Sample", "Traditional / Kalaam", "Traditional")
                ) {
                    naatDao.deletePermanently(item.id)
                    deleteFromFirestore(item.id)
                }
            }

            val fs = firestore
            if (fs != null) {
                fs.collection("naats").get().addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        repositoryScope.launch {
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val titleHindi = doc.getString("titleHindi") ?: ""
                                val titleEnglish = doc.getString("titleEnglish") ?: ""
                                val addedBy = doc.getString("addedBy") ?: ""
                                if (DefaultNaatData.isDefaultNaat(id, titleHindi, titleEnglish) &&
                                    addedBy in listOf("Admin", "System", "Default", "Sample", "Traditional / Kalaam", "Traditional")
                                ) {
                                    naatDao.deletePermanently(id)
                                    deleteFromFirestore(id)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NaatRepository", "Database purge error: ${e.message}")
        }
        Unit
    }

    suspend fun clearAllNaats() = withContext(Dispatchers.IO) {
        try {
            naatDao.clearAllNaats()
            val fs = firestore
            if (fs != null) {
                fs.collection("naats").get().addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            deleteFromFirestore(doc.id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NaatRepository", "Clear all naats error: ${e.message}")
        }
        Unit
    }

    suspend fun initializeDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        purgeAllDefaultNaats()
    }

    fun getNaatsByCategory(category: String): Flow<List<NaatEntity>> = combine(
        naatDao.getNaatsByCategory(category),
        userFavoriteIds
    ) { naats, favIds ->
        naats.map { naat ->
            naat.copy(isFavorite = favIds.contains(naat.id))
        }
    }

    suspend fun cacheLyrics(naat: NaatEntity) = withContext(Dispatchers.IO) {
        naatDao.insertOrUpdate(naat)
    }

    suspend fun getNaatById(id: String): NaatEntity? = withContext(Dispatchers.IO) {
        val naat = naatDao.getNaatById(id) ?: return@withContext null
        val email = preferences.getCurrentUserEmail()
        val isFav = userFavoriteDao.isFavoriteSync(email, id)
        naat.copy(isFavorite = isFav)
    }

    fun observeNaatById(id: String): Flow<NaatEntity?> = combine(
        naatDao.observeNaatById(id),
        userFavoriteIds
    ) { naat, favIds ->
        naat?.copy(isFavorite = favIds.contains(id))
    }

    suspend fun toggleFavorite(id: String, explicitUserEmail: String? = null) = withContext(Dispatchers.IO) {
        val email = if (!explicitUserEmail.isNullOrBlank()) {
            explicitUserEmail.trim().lowercase()
        } else {
            preferences.getCurrentUserEmail()
        }
        val isFav = userFavoriteDao.isFavoriteSync(email, id)
        if (isFav) {
            userFavoriteDao.removeFavorite(email, id)
            deleteUserFavoriteFromFirestore(email, id)
        } else {
            userFavoriteDao.addFavorite(UserFavoriteEntity(userEmail = email, naatId = id))
            syncUserFavoriteToFirestore(email, id)
        }
    }

    suspend fun toggleFavorite(id: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        toggleFavorite(id)
    }

    suspend fun saveNaat(naat: NaatEntity) = withContext(Dispatchers.IO) {
        val entity = if (naat.id.isEmpty()) {
            naat.copy(id = "naat_" + UUID.randomUUID().toString().take(8))
        } else {
            naat.copy(updatedAt = System.currentTimeMillis())
        }
        naatDao.insertOrUpdate(entity)
        syncToFirestore(entity)
    }

    private val permanentlyDeletedIds = mutableSetOf<String>()
    private val recentlyUpdatedIds = mutableMapOf<String, Long>()

    suspend fun softDelete(id: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        recentlyUpdatedIds[id] = now
        naatDao.softDelete(id, now)
        try {
            firestore?.collection("naats")?.document(id)?.update(
                mapOf(
                    "isDeleted" to true,
                    "updatedAt" to now
                )
            )
        } catch (e: Exception) {
            Log.e("NaatRepository", "Soft delete failed: ${e.message}")
        }
    }

    suspend fun restoreFromTrash(id: String) = withContext(Dispatchers.IO) {
        naatDao.restoreFromTrash(id)
        val item = naatDao.getNaatById(id)
        item?.let { syncToFirestore(it) }
    }

    suspend fun deletePermanently(id: String) = withContext(Dispatchers.IO) {
        permanentlyDeletedIds.add(id)
        naatDao.deletePermanently(id)
        deleteFromFirestore(id)
    }

    suspend fun emptyTrashBin() = withContext(Dispatchers.IO) {
        val trashedNaats = naatDao.getDeletedNaatsSync()
        for (naat in trashedNaats) {
            deleteFromFirestore(naat.id)
        }
        naatDao.emptyTrashBin()
    }

    suspend fun approveNaat(id: String) = withContext(Dispatchers.IO) {
        try {
            recentlyUpdatedIds[id] = System.currentTimeMillis()
            val item = naatDao.getNaatById(id)
            if (item != null) {
                val approved = item.copy(status = "APPROVED", updatedAt = System.currentTimeMillis())
                naatDao.insertOrUpdate(approved)
                syncToFirestore(approved)
            } else {
                Log.w("NaatRepository", "approveNaat: Item not found with id $id")
            }
        } catch (e: Exception) {
            Log.e("NaatRepository", "approveNaat failed: ${e.message}")
        }
    }

    /**
     * Upload / sync single Naat entity to Cloud Firestore
     */
    fun syncToFirestore(naat: NaatEntity) {
        try {
            firestore?.collection("naats")?.document(naat.id)?.set(
                mapOf(
                    "id" to naat.id,
                    "titleHindi" to naat.titleHindi,
                    "titleGujarati" to naat.titleGujarati,
                    "titleEnglish" to naat.titleEnglish,
                    "lyricsHindi" to naat.lyricsHindi,
                    "lyricsGujarati" to naat.lyricsGujarati,
                    "lyricsEnglish" to naat.lyricsEnglish,
                    "category" to naat.category,
                    "poet" to naat.poet,
                    "audioUrl" to naat.audioUrl,
                    "orderIndex" to naat.orderIndex,
                    "isDeleted" to naat.isDeleted,
                    "addedBy" to naat.addedBy,
                    "createdAt" to naat.createdAt,
                    "updatedAt" to naat.updatedAt,
                    "status" to naat.status
                )
            )?.addOnSuccessListener {
                Log.i("NaatRepository", "Firestore sync success: ${naat.id} (${naat.titleHindi})")
            }?.addOnFailureListener { e ->
                Log.w("NaatRepository", "Firestore sync skipped or failed: ${e.message}")
            }
        } catch (e: Throwable) {
            Log.w("NaatRepository", "Firestore operation ignored gracefully: ${e.message}")
        }
    }

    private fun deleteFromFirestore(id: String) {
        try {
            firestore?.collection("naats")?.document(id)?.delete()
                ?.addOnSuccessListener {
                    Log.i("NaatRepository", "Firestore delete success: $id")
                }
                ?.addOnFailureListener { e ->
                    Log.w("NaatRepository", "Firestore delete failed: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.w("NaatRepository", "Firestore delete failed gracefully: ${e.message}")
        }
    }

    fun isFirestoreConnected(): Boolean {
        return firestore != null
    }
}
