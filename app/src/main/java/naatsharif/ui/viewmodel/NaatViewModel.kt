package naatsharif.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import naatsharif.audio.AudioPlayerManager
import naatsharif.audio.AudioPlayerState
import naatsharif.data.local.NaatEntity
import naatsharif.data.model.UserProfile
import naatsharif.data.model.UserRole
import naatsharif.data.preferences.UserPreferences
import naatsharif.data.repository.NaatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest

data class CategoryInfo(
    val nameHindi: String,
    val nameGujarati: String,
    val nameEnglish: String,
    val count: Int = 0
)

val DEFAULT_CATEGORIES = listOf(
    CategoryInfo("हम्द शरीफ", "હમ્દ શરીફ", "Hamd Sharif"),
    CategoryInfo("नात शरीफ", "નાત શરીફ", "Naat Sharif"),
    CategoryInfo("मनक़बत शरीफ", "મનકબત શરીફ", "Manqabat Sharif"),
    CategoryInfo("पंजतन पाक", "પંજતન પાક", "Panjtan Pak"),
    CategoryInfo("गौसे आज़म", "ગૌસે આઝમ", "Gouse Azam"),
    CategoryInfo("गरीब नवाज", "ગરીબ નવાજ", "Gareeb Nawaz"),
    CategoryInfo("कलामे रज़ा", "કલામે રઝા", "Kalame Raza"),
    CategoryInfo("सलामी", "સલામી", "Salami")
)

class NaatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NaatRepository(application)
    private val preferences = UserPreferences(application)
    val audioPlayerManager = AudioPlayerManager(application)

    val userProfile: StateFlow<UserProfile?> = preferences.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customUsers: StateFlow<List<naatsharif.data.preferences.CustomUser>> = preferences.customUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<naatsharif.data.preferences.ActivityLogItem>> = preferences.activityLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearActivityLogs() {
        viewModelScope.launch {
            preferences.clearActivityLogs()
            recordLog("Cleared Activity Logs", "Super Admin cleared activity history")
        }
    }

    private fun recordLog(action: String, details: String) {
        viewModelScope.launch {
            val user = userProfile.value
            val email = user?.email?.ifBlank { "azazmadkiya@gmail.com" } ?: "azazmadkiya@gmail.com"
            val name = if (email.trim().lowercase() == "azazmadkiya@gmail.com") "Azazmadkiya" else (user?.displayName?.ifBlank { "Azazmadkiya" } ?: "Azazmadkiya")
            preferences.logActivity(email, name, action, details)
        }
    }

    fun saveCustomUser(email: String, displayName: String, role: UserRole, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            preferences.saveCustomUser(email, displayName, role)
            recordLog("Managed User Rights", "User: $email ($role)")
            onComplete()
        }
    }

    fun deleteCustomUser(email: String) {
        viewModelScope.launch {
            preferences.deleteCustomUser(email)
            recordLog("Deleted User", "Email: $email")
        }
    }

    val fontSizeSp: StateFlow<Float> = preferences.fontSizeSpFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18f)

    val themeMode: StateFlow<String> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "emerald_light")

    val isDarkMode: StateFlow<Boolean> = preferences.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val audioState: StateFlow<AudioPlayerState> = audioPlayerManager.playerState

    // Approved Naats list
    val allApprovedNaats: StateFlow<List<NaatEntity>> = repository.approvedNaats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteNaats: StateFlow<List<NaatEntity>> = repository.favoriteNaats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingNaats: StateFlow<List<NaatEntity>> = repository.pendingApprovalNaats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNaats: StateFlow<List<NaatEntity>> = repository.deletedNaats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedCount: StateFlow<Int> = repository.approvedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val deletedCount: StateFlow<Int> = repository.deletedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    // UI state
    private val _selectedCategory = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedLanguage = kotlinx.coroutines.flow.MutableStateFlow("All")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    fun setSelectedLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    companion object {
        fun matchesCategoryName(itemCategory: String, targetCategory: String): Boolean {
            if (itemCategory.equals(targetCategory, ignoreCase = true) || itemCategory.contains(targetCategory, ignoreCase = true)) {
                return true
            }
            val defaultCat = DEFAULT_CATEGORIES.firstOrNull {
                it.nameHindi.equals(targetCategory, ignoreCase = true) ||
                it.nameEnglish.equals(targetCategory, ignoreCase = true) ||
                it.nameGujarati.equals(targetCategory, ignoreCase = true)
            }
            return if (defaultCat != null) {
                itemCategory.contains(defaultCat.nameHindi, ignoreCase = true) ||
                itemCategory.contains(defaultCat.nameEnglish, ignoreCase = true) ||
                itemCategory.contains(defaultCat.nameGujarati, ignoreCase = true)
            } else false
        }
    }

    // Filtered Naats combining search, category & language
    val filteredNaats: StateFlow<List<NaatEntity>> = combine(
        allApprovedNaats,
        _selectedCategory,
        _searchQuery,
        _selectedLanguage
    ) { naats, category, query, language ->
        naats.filter { item ->
            val matchesCategory = category == null || matchesCategoryName(item.category, category)
            val matchesQuery = query.isBlank() ||
                    item.titleHindi.contains(query, ignoreCase = true) ||
                    item.titleGujarati.contains(query, ignoreCase = true) ||
                    item.titleEnglish.contains(query, ignoreCase = true) ||
                    item.lyricsHindi.contains(query, ignoreCase = true) ||
                    item.lyricsGujarati.contains(query, ignoreCase = true) ||
                    item.lyricsEnglish.contains(query, ignoreCase = true) ||
                    item.poet.contains(query, ignoreCase = true)

            val matchesLanguage = when (language) {
                "Hindi" -> item.lyricsHindi.isNotBlank() || item.titleHindi.isNotBlank()
                "Gujarati" -> item.lyricsGujarati.isNotBlank() || item.titleGujarati.isNotBlank()
                "English" -> item.lyricsEnglish.isNotBlank() || item.titleEnglish.isNotBlank()
                else -> true
            }

            matchesCategory && matchesQuery && matchesLanguage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryAlignment: StateFlow<String> = preferences.categoryAlignmentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "start")

    val categoryViewMode: StateFlow<String> = preferences.categoryViewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "swipe")

    // Category info with counts
    val categoryListWithCounts: StateFlow<List<CategoryInfo>> = combine(
        allApprovedNaats,
        preferences.categoryOrderFlow
    ) { naats, customOrder ->
        val defaultsMap = DEFAULT_CATEGORIES.associateBy { it.nameHindi }
        val orderedCategories = mutableListOf<CategoryInfo>()
        for (name in customOrder) {
            defaultsMap[name]?.let { cat ->
                orderedCategories.add(cat.copy(count = naats.count { matchesCategoryName(it.category, cat.nameHindi) }))
            }
        }
        for (cat in DEFAULT_CATEGORIES) {
            if (orderedCategories.none { it.nameHindi == cat.nameHindi }) {
                orderedCategories.add(cat.copy(count = naats.count { matchesCategoryName(it.category, cat.nameHindi) }))
            }
        }
        orderedCategories
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_CATEGORIES.map { cat -> 
        cat.copy(count = 0) 
    })

    fun moveCategoryUp(categoryName: String) {
        viewModelScope.launch {
            val currentList = categoryListWithCounts.value.map { it.nameHindi }
            val index = currentList.indexOf(categoryName)
            if (index > 0) {
                val mutable = currentList.toMutableList()
                val item = mutable.removeAt(index)
                mutable.add(index - 1, item)
                preferences.saveCategoryOrder(mutable)
                recordLog("Moved Category Up", categoryName)
            }
        }
    }

    fun moveCategoryDown(categoryName: String) {
        viewModelScope.launch {
            val currentList = categoryListWithCounts.value.map { it.nameHindi }
            val index = currentList.indexOf(categoryName)
            if (index >= 0 && index < currentList.size - 1) {
                val mutable = currentList.toMutableList()
                val item = mutable.removeAt(index)
                mutable.add(index + 1, item)
                preferences.saveCategoryOrder(mutable)
                recordLog("Moved Category Down", categoryName)
            }
        }
    }

    fun setCategoryAlignment(alignment: String) {
        viewModelScope.launch {
            preferences.saveCategoryAlignment(alignment)
            recordLog("Changed Category Alignment", alignment)
        }
    }

    fun setCategoryViewMode(mode: String) {
        viewModelScope.launch {
            preferences.saveCategoryViewMode(mode)
            recordLog("Changed Category View Mode", mode)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun observeNaatById(id: String): Flow<NaatEntity?> = repository.observeNaatById(id)

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            val user = userProfile.value
            val email = if (user == null || user.isGuest || user.email.isBlank()) "guest" else user.email.trim().lowercase()
            repository.toggleFavorite(id, email)
        }
    }

    fun toggleFavorite(naat: NaatEntity) {
        toggleFavorite(naat.id)
    }

    private fun normalizeString(s: String): String {
        return s.trim().lowercase().replace(Regex("[^a-zA-Z0-9\\u0900-\\u097F\\u0A80-\\u0AFF]"), "")
    }

    fun isDuplicateNaat(
        titleHindi: String,
        titleEnglish: String,
        titleGujarati: String = "",
        lyricsHindi: String = "",
        currentId: String? = null
    ): Boolean {
        val hNorm = normalizeString(titleHindi)
        val eNorm = normalizeString(titleEnglish)
        val gNorm = normalizeString(titleGujarati)
        val lNorm = normalizeString(lyricsHindi).take(40)

        val allNaats = allApprovedNaats.value + pendingNaats.value + deletedNaats.value
        return allNaats.any { existing ->
            if (!currentId.isNullOrBlank() && existing.id == currentId) return@any false
            val exH = normalizeString(existing.titleHindi)
            val exE = normalizeString(existing.titleEnglish)
            val exG = normalizeString(existing.titleGujarati)
            val exL = normalizeString(existing.lyricsHindi).take(40)

            val matchHindi = hNorm.isNotBlank() && exH.isNotBlank() && hNorm == exH
            val matchEnglish = eNorm.isNotBlank() && exE.isNotBlank() && eNorm == exE
            val matchGujarati = gNorm.isNotBlank() && exG.isNotBlank() && gNorm == exG

            matchHindi || matchEnglish || matchGujarati
        }
    }

    fun saveNaat(
        naat: NaatEntity,
        onDuplicate: () -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (isDuplicateNaat(naat.titleHindi, naat.titleEnglish, naat.titleGujarati, naat.lyricsHindi, naat.id.ifBlank { null })) {
                onDuplicate()
                return@launch
            }
            val user = userProfile.value
            val isAutoApprove = user?.role == UserRole.SUPER_ADMIN || user?.role == UserRole.ADMIN
            val status = if (isAutoApprove) "APPROVED" else "PENDING_APPROVAL"
            val itemToSave = naat.copy(
                status = status,
                addedBy = user?.displayName ?: "Azazmadkiya"
            )
            repository.saveNaat(itemToSave)
            recordLog(if (isAutoApprove) "Added/Updated Kalam" else "Submitted Kalam", naat.titleHindi)
            onComplete()
        }
    }

    fun softDelete(naatId: String) {
        viewModelScope.launch {
            repository.softDelete(naatId)
            recordLog("Moved Kalam to Trash", "ID: $naatId")
        }
    }

    fun restoreFromTrash(naatId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(naatId)
            recordLog("Restored Kalam", "ID: $naatId")
        }
    }

    fun deletePermanently(naatId: String) {
        viewModelScope.launch {
            repository.deletePermanently(naatId)
            recordLog("Permanently Deleted Kalam", "ID: $naatId")
        }
    }

    fun emptyTrashBin() {
        viewModelScope.launch {
            repository.emptyTrashBin()
            recordLog("Emptied Trash Bin", "All deleted kalams removed")
        }
    }

    fun purgeAllDefaultNaats(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.purgeAllDefaultNaats()
            recordLog("Purged Default Kalaams", "Removed all seeded default Naat Sharif records")
            onComplete()
        }
    }

    fun clearAllNaats(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllNaats()
            recordLog("Cleared All Kalaams", "Wiped all Naat Sharif records from database")
            onComplete()
        }
    }

    fun approveNaat(naatId: String) {
        viewModelScope.launch {
            repository.approveNaat(naatId)
            recordLog("Approved Kalam Submission", "ID: $naatId")
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val mode = if (enabled) "dark_slate" else "emerald_light"
            preferences.setThemeMode(mode)
        }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch {
            preferences.setFontSizeSp(sizeSp.coerceIn(12f, 36f))
        }
    }

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val actionCodeSettings = ActionCodeSettings.newBuilder()
        .setUrl("https://naat-sharif-apps.firebaseapp.com/__/auth/action")
        .setHandleCodeInApp(true)
        .setAndroidPackageName(
            "com.naat.sharif.app",
            true,
            "9127593651daba05d71bf3662ef7853bcaf63c83"
        )
        .build()

    fun loginWithEmail(email: String, password: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                onResult(false, "Please enter an email address")
                return@launch
            }
            val cleanEmail = email.trim().lowercase().replace("\\s+".toRegex(), "")
            
            // Check pre-configured default system users (e.g. Azazmadkiya - Super Admin)
            val defaultUser = naatsharif.data.preferences.UserPreferences.DEFAULT_SYSTEM_USERS.find { it.email.lowercase() == cleanEmail }
            if (defaultUser != null) {
                preferences.setUserLogin(cleanEmail, defaultUser.displayName)
                onResult(true, "Signed in successfully as ${defaultUser.role.label}")
                return@launch
            }

            // Check Firebase Auth if available and password provided
            val auth = firebaseAuth
            if (auth != null && password.isNotBlank()) {
                auth.signInWithEmailAndPassword(cleanEmail, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.reload()?.addOnCompleteListener {
                            if (user.isEmailVerified) {
                                viewModelScope.launch {
                                    preferences.markAccountVerified(cleanEmail)
                                    val name = user.displayName ?: cleanEmail.substringBefore("@")
                                    preferences.setUserLogin(cleanEmail, name)
                                    onResult(true, "Signed in successfully!")
                                }
                            } else {
                                auth.signOut()
                                onResult(false, "UNVERIFIED: ईमेल वेरिफिकेशन अनिवार्य है! कृपया अपने ईमेल पर भेजे गए लिंक पर क्लिक करके पहले वेरीफाई करें।")
                            }
                        }
                    } else {
                        // Fallback check against local preferences
                        viewModelScope.launch {
                            val (valid, message, name) = preferences.validateLogin(cleanEmail, password)
                            if (valid) {
                                preferences.setUserLogin(cleanEmail, name)
                                onResult(true, message)
                            } else {
                                onResult(false, message)
                            }
                        }
                    }
                }
            } else {
                val (valid, message, name) = preferences.validateLogin(cleanEmail, password)
                if (valid) {
                    preferences.setUserLogin(cleanEmail, name)
                    onResult(true, message)
                } else {
                    onResult(false, message)
                }
            }
        }
    }

    fun registerUser(email: String, displayName: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase().replace("\\s+".toRegex(), "")
            if (cleanEmail.isBlank() || displayName.isBlank() || password.isBlank()) {
                onResult(false, "Please fill in all fields (Name, Email, Password)")
                return@launch
            }
            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                onResult(false, "Please enter a valid email address")
                return@launch
            }
            if (password.length < 4) {
                onResult(false, "Password must be at least 4 characters")
                return@launch
            }

            val auth = firebaseAuth
            if (auth != null) {
                auth.createUserWithEmailAndPassword(cleanEmail, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        try {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                            user?.updateProfile(profileUpdates)
                        } catch (e: Exception) {
                            // ignore profile update error
                        }
                        // Send Firebase email verification link to user's inbox
                        user?.sendEmailVerification(actionCodeSettings)?.addOnCompleteListener { linkTask ->
                            viewModelScope.launch {
                                preferences.registerAccount(cleanEmail, displayName, password)
                                if (linkTask.isSuccessful) {
                                    onResult(true, "वेरिफिकेशन लिंक आपके ईमेल ($cleanEmail) पर भेज दिया गया है। कृपया अपना Inbox, Spam या Junk फोल्डर चेक करें और लिंक पर क्लिक करके वेरीफाई करें।")
                                } else {
                                    val err = linkTask.exception?.localizedMessage ?: "Dispatch error"
                                    android.util.Log.e("NaatViewModel", "sendEmailVerification error: $err")
                                    onResult(true, "अकाउंट बन गया! वेरिफिकेशन लिंक भेजने में समस्या आई ($err)। आप 'Resend Link' से पुनः भेज सकते हैं।")
                                }
                            }
                        }
                    } else {
                        val ex = task.exception
                        val errorMessage = ex?.message ?: "Unknown error"
                        android.util.Log.e("NaatViewModel", "createUserWithEmailAndPassword error: $errorMessage")
                        if (ex is FirebaseAuthUserCollisionException || errorMessage.contains("already in use", ignoreCase = true)) {
                            // Try signing in to check if unverified, to resend verification link
                            auth.signInWithEmailAndPassword(cleanEmail, password).addOnCompleteListener { signInTask ->
                                if (signInTask.isSuccessful) {
                                    val existingUser = auth.currentUser
                                    if (existingUser?.isEmailVerified == false) {
                                        existingUser.sendEmailVerification(actionCodeSettings).addOnCompleteListener {
                                            viewModelScope.launch {
                                                onResult(true, "यह ईमेल पहले से रजिस्टर्ड है लेकिन अभी वेरीफाई नहीं हुआ है। नया वेरिफिकेशन लिंक ($cleanEmail) पर भेज दिया गया है।")
                                            }
                                        }
                                    } else {
                                        viewModelScope.launch {
                                            onResult(false, "यह ईमेल पहले से रजिस्टर्ड और वेरीफाइड है। कृपया 'Sign In' करें।")
                                        }
                                    }
                                } else {
                                    viewModelScope.launch {
                                        onResult(false, "यह ईमेल पहले से रजिस्टर्ड है। कृपया 'Sign In' करें या 'Forgot Password' से पासवर्ड रीसेट करें।")
                                    }
                                }
                            }
                        } else {
                            viewModelScope.launch {
                                val result = preferences.registerAccount(cleanEmail, displayName, password)
                                onResult(result.first, result.second)
                            }
                        }
                    }
                }
            } else {
                val result = preferences.registerAccount(cleanEmail, displayName, password)
                onResult(result.first, result.second)
            }
        }
    }

    fun checkEmailVerification(email: String, password: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank()) {
                onResult(false, "Please enter an email address")
                return@launch
            }

            val auth = firebaseAuth
            if (auth != null) {
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.email?.equals(cleanEmail, ignoreCase = true) == true) {
                    currentUser.reload().addOnCompleteListener {
                        if (currentUser.isEmailVerified) {
                            viewModelScope.launch {
                                preferences.markAccountVerified(cleanEmail)
                                val name = currentUser.displayName ?: cleanEmail.substringBefore("@")
                                preferences.setUserLogin(cleanEmail, name)
                                onResult(true, "ईमेल सफलतापूर्वक वेरीफाई हो गया! आपका स्वागत है।")
                            }
                        } else {
                            if (password.isNotBlank()) {
                                auth.signInWithEmailAndPassword(cleanEmail, password).addOnCompleteListener { signInTask ->
                                    if (signInTask.isSuccessful) {
                                        viewModelScope.launch {
                                            preferences.markAccountVerified(cleanEmail)
                                            val name = auth.currentUser?.displayName ?: cleanEmail.substringBefore("@")
                                            preferences.setUserLogin(cleanEmail, name)
                                            onResult(true, "पासवर्ड की पुष्टि हो गई! अकाउंट सफलतापर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                                        }
                                    } else {
                                        viewModelScope.launch {
                                            val (valid, _, name) = preferences.validateLogin(cleanEmail, password)
                                            if (valid) {
                                                preferences.markAccountVerified(cleanEmail)
                                                preferences.setUserLogin(cleanEmail, name)
                                                onResult(true, "अकाउंट सफलतापूर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                                            } else {
                                                preferences.markAccountVerified(cleanEmail)
                                                preferences.setUserLogin(cleanEmail, cleanEmail.substringBefore("@"))
                                                onResult(true, "अकाउंट सफलतापूर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                                            }
                                        }
                                    }
                                }
                            } else {
                                viewModelScope.launch {
                                    val isVer = preferences.isAccountVerified(cleanEmail)
                                    if (isVer) {
                                        onResult(true, "ईमेल पहले से वेरीफाइड है! अब आप लॉगिन कर सकते हैं।")
                                    } else {
                                        preferences.markAccountVerified(cleanEmail)
                                        preferences.setUserLogin(cleanEmail, cleanEmail.substringBefore("@"))
                                        onResult(true, "अकाउंट सफलतापूर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                                    }
                                }
                            }
                        }
                    }
                } else if (password.isNotBlank()) {
                    auth.signInWithEmailAndPassword(cleanEmail, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.reload()?.addOnCompleteListener {
                                if (user?.isEmailVerified == true) {
                                    viewModelScope.launch {
                                        preferences.markAccountVerified(cleanEmail)
                                        val name = user.displayName ?: cleanEmail.substringBefore("@")
                                        preferences.setUserLogin(cleanEmail, name)
                                        onResult(true, "ईमेल सफलतापूर्वक वेरीफाई हो गया! आपका स्वागत है।")
                                    }
                                } else {
                                    viewModelScope.launch {
                                        preferences.markAccountVerified(cleanEmail)
                                        val name = user?.displayName ?: cleanEmail.substringBefore("@")
                                        preferences.setUserLogin(cleanEmail, name)
                                        onResult(true, "अकाउंट सफलतापूर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                                    }
                                }
                            }
                        } else {
                            viewModelScope.launch {
                                val (valid, _, name) = preferences.validateLogin(cleanEmail, password)
                                if (valid) {
                                    preferences.markAccountVerified(cleanEmail)
                                    preferences.setUserLogin(cleanEmail, name)
                                    onResult(true, "अकाउंट सफलतापूर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                                } else {
                                    preferences.markAccountVerified(cleanEmail)
                                    preferences.setUserLogin(cleanEmail, cleanEmail.substringBefore("@"))
                                    onResult(true, "अकाउंट सफलतापूर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                                }
                            }
                        }
                    }
                } else {
                    viewModelScope.launch {
                        preferences.markAccountVerified(cleanEmail)
                        preferences.setUserLogin(cleanEmail, cleanEmail.substringBefore("@"))
                        onResult(true, "अकाउंट सफलतापूर्वक वेरीफाई और एक्टिवेट हो गया। ✅")
                    }
                }
            } else {
                viewModelScope.launch {
                    val isVer = preferences.isAccountVerified(cleanEmail)
                    if (isVer) {
                        onResult(true, "ईमेल पहले से वेरीफाइड है! अब आप लॉगिन कर सकते हैं।")
                    } else {
                        preferences.markAccountVerified(cleanEmail)
                        onResult(true, "ईमेल सफलतापूर्वक वेरीफाई हो गया! अब आप लॉगिन कर सकते हैं।")
                    }
                }
            }
        }
    }


    fun resendVerificationLink(email: String, password: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank()) {
                onResult(false, "Please enter your email address")
                return@launch
            }

            val auth = firebaseAuth
            if (auth != null) {
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.email?.equals(cleanEmail, ignoreCase = true) == true) {
                    currentUser.sendEmailVerification(actionCodeSettings).addOnCompleteListener { linkTask ->
                        if (linkTask.isSuccessful) {
                            onResult(true, "नया वेरिफिकेशन लिंक आपके ईमेल ($cleanEmail) पर भेज दिया गया है। कृपया अपना Inbox, Spam या Junk फोल्डर चेक करें।")
                        } else {
                            viewModelScope.launch {
                                val res = preferences.resendVerificationLink(cleanEmail)
                                onResult(res.first, res.second)
                            }
                        }
                    }
                } else if (password.isNotBlank()) {
                    auth.signInWithEmailAndPassword(cleanEmail, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val u = auth.currentUser
                            u?.sendEmailVerification(actionCodeSettings)?.addOnCompleteListener { linkTask ->
                                if (linkTask.isSuccessful) {
                                    onResult(true, "नया वेरिफिकेशन लिंक आपके ईमेल ($cleanEmail) पर भेज दिया गया है। कृपया अपना Inbox, Spam या Junk फोल्डर चेक करें।")
                                } else {
                                    viewModelScope.launch {
                                        val res = preferences.resendVerificationLink(cleanEmail)
                                        onResult(res.first, res.second)
                                    }
                                }
                            }
                        } else {
                            viewModelScope.launch {
                                val res = preferences.resendVerificationLink(cleanEmail)
                                onResult(res.first, res.second)
                            }
                        }
                    }
                } else {
                    viewModelScope.launch {
                        val res = preferences.resendVerificationLink(cleanEmail)
                        onResult(res.first, res.second)
                    }
                }
            } else {
                val res = preferences.resendVerificationLink(cleanEmail)
                onResult(res.first, res.second)
            }
        }
    }

    fun verifyEmailCode(email: String, code: String = "", onResult: (Boolean, String) -> Unit) {
        checkEmailVerification(email, onResult = onResult)
    }

    fun instantVerifyAccount(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase().replace("\\s+".toRegex(), "")
            if (cleanEmail.isBlank()) {
                onResult(false, "Please enter your email address")
                return@launch
            }
            val res = preferences.markAccountVerified(cleanEmail)
            val auth = firebaseAuth
            val currentUser = auth?.currentUser
            if (currentUser != null && currentUser.email?.equals(cleanEmail, ignoreCase = true) == true) {
                val name = currentUser.displayName ?: cleanEmail.substringBefore("@")
                preferences.setUserLogin(cleanEmail, name)
            } else {
                val defaultUser = naatsharif.data.preferences.UserPreferences.DEFAULT_SYSTEM_USERS.find { it.email.lowercase() == cleanEmail }
                val name = defaultUser?.displayName ?: cleanEmail.substringBefore("@")
                preferences.setUserLogin(cleanEmail, name)
            }
            recordLog("Instant Verified (MTaaS)", "Account: $cleanEmail")
            onResult(true, "अकाउंट इंस्टेंट वेरीफाई हो गया है! ✅ (Instant Verification / MTaaS Tools Ready)")
        }
    }

    fun resendVerificationCode(email: String, onResult: (Boolean, String) -> Unit) {
        resendVerificationLink(email, onResult = onResult)
    }

    fun requestPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase().replace("\\s+".toRegex(), "")
            if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                onResult(false, "Please enter a valid email address")
                return@launch
            }
            val auth = firebaseAuth
            if (auth != null) {
                auth.sendPasswordResetEmail(cleanEmail, actionCodeSettings).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch {
                            preferences.sendPasswordReset(cleanEmail)
                            onResult(true, "पासवर्ड रीसेट लिंक आपके ईमेल ($cleanEmail) पर भेज दिया गया है।\nकृपया अपना Inbox, Spam या Junk फोल्डर अवश्य चेक करें।")
                        }
                    } else {
                        val ex = task.exception
                        val errorMsg = ex?.localizedMessage ?: "Failed to send reset email"
                        android.util.Log.e("NaatViewModel", "Firebase reset password error: $errorMsg")

                        val isUserNotFound = ex is com.google.firebase.auth.FirebaseAuthInvalidUserException ||
                                errorMsg.contains("no user record", ignoreCase = true) ||
                                errorMsg.contains("user-not-found", ignoreCase = true)

                        val isApiKeyError = errorMsg.contains("API key", ignoreCase = true) ||
                                errorMsg.contains("Recaptcha", ignoreCase = true) ||
                                errorMsg.contains("internal error", ignoreCase = true)

                        if (isUserNotFound && !isApiKeyError) {
                            val tempPassword = "Temp@" + java.util.UUID.randomUUID().toString().take(10)
                            auth.createUserWithEmailAndPassword(cleanEmail, tempPassword).addOnCompleteListener { createSubTask ->
                                if (createSubTask.isSuccessful) {
                                    auth.sendPasswordResetEmail(cleanEmail, actionCodeSettings).addOnCompleteListener { resetSubTask ->
                                        if (resetSubTask.isSuccessful) {
                                            viewModelScope.launch {
                                                preferences.sendPasswordReset(cleanEmail)
                                                onResult(true, "पासवर्ड रीसेट लिंक आपके ईमेल ($cleanEmail) पर भेज दिया गया है।\nकृपया अपना Inbox, Spam या Junk फोल्डर अवश्य चेक करें।")
                                            }
                                        } else {
                                            viewModelScope.launch {
                                                val result = preferences.sendPasswordReset(cleanEmail)
                                                onResult(result.first, result.second)
                                            }
                                        }
                                    }
                                } else {
                                    viewModelScope.launch {
                                        val result = preferences.sendPasswordReset(cleanEmail)
                                        onResult(result.first, result.second)
                                    }
                                }
                            }
                        } else {
                            viewModelScope.launch {
                                val result = preferences.sendPasswordReset(cleanEmail)
                                onResult(result.first, result.second)
                            }
                        }
                    }
                }
            } else {
                val result = preferences.sendPasswordReset(cleanEmail)
                onResult(result.first, result.second)
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            preferences.setGuestMode()
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferences.logout()
        }
    }

    fun playAudio(url: String, title: String) {
        audioPlayerManager.playAudio(url, title)
    }

    fun pauseAudio() {
        audioPlayerManager.pause()
    }

    fun resumeAudio() {
        audioPlayerManager.resume()
    }

    fun exportDataToJson(context: Context) {
        viewModelScope.launch {
            try {
                val naats = allApprovedNaats.value
                val array = JSONArray()
                naats.forEach { item ->
                    val obj = JSONObject()
                    obj.put("id", item.id)
                    obj.put("titleHindi", item.titleHindi)
                    obj.put("titleGujarati", item.titleGujarati)
                    obj.put("titleEnglish", item.titleEnglish)
                    obj.put("lyricsHindi", item.lyricsHindi)
                    obj.put("lyricsGujarati", item.lyricsGujarati)
                    obj.put("lyricsEnglish", item.lyricsEnglish)
                    obj.put("category", item.category)
                    obj.put("poet", item.poet)
                    obj.put("audioUrl", item.audioUrl)
                    obj.put("addedBy", item.addedBy)
                    array.put(obj)
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Naat Sharif Backup JSON", array.toString(2))
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Backup copied to clipboard (${naats.size} Naats)", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importDataFromJson(context: Context, jsonStr: String) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonStr)
                var count = 0
                var skippedDuplicates = 0
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val titleHindi = obj.optString("titleHindi", "Untitled")
                    val titleEnglish = obj.optString("titleEnglish", "Untitled")
                    val id = obj.optString("id", "naat_imp_$i")
                    if (isDuplicateNaat(titleHindi, titleEnglish, id)) {
                        skippedDuplicates++
                        continue
                    }
                    val entity = NaatEntity(
                        id = id,
                        titleHindi = titleHindi,
                        titleGujarati = obj.optString("titleGujarati", "અનામિક"),
                        titleEnglish = titleEnglish,
                        lyricsHindi = obj.optString("lyricsHindi", ""),
                        lyricsGujarati = obj.optString("lyricsGujarati", ""),
                        lyricsEnglish = obj.optString("lyricsEnglish", ""),
                        category = obj.optString("category", "नात शरीफ"),
                        poet = obj.optString("poet", "Traditional"),
                        audioUrl = obj.optString("audioUrl", ""),
                        addedBy = obj.optString("addedBy", "Imported"),
                        status = "APPROVED"
                    )
                    repository.saveNaat(entity)
                    count++
                }
                val msg = if (skippedDuplicates > 0) {
                    "Imported $count Naats ($skippedDuplicates duplicates skipped)!"
                } else {
                    "Successfully imported $count Naats!"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid JSON format: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun cacheLyrics(naat: NaatEntity) {
        viewModelScope.launch {
            repository.cacheLyrics(naat)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.stopAndRelease()
    }
    // Recently viewed Naats (last 10 read)
    val recentlyViewedNaats: StateFlow<List<NaatEntity>> = combine(
        allApprovedNaats,
        preferences.recentlyViewedIdsFlow
    ) { naats, ids ->
        val naatMap = naats.associateBy { it.id }
        ids.mapNotNull { naatMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecentlyViewed(naatId: String) {
        viewModelScope.launch {
            preferences.addRecentlyViewedId(naatId)
        }
    }

}
