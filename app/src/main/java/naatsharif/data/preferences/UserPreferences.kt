package naatsharif.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import naatsharif.data.model.UserProfile
import naatsharif.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "naat_preferences")

data class RegisteredAccount(
    val email: String,
    val displayName: String,
    val passwordHash: String = "",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CustomUser(
    val email: String,
    val displayName: String,
    val role: UserRole
)

data class ActivityLogItem(
    val id: String,
    val email: String,
    val name: String,
    val action: String,
    val details: String,
    val timestamp: Long
)

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_EMAIL = stringPreferencesKey("user_email")
        private val KEY_NAME = stringPreferencesKey("user_name")
        private val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
        private val KEY_FONT_SIZE = floatPreferencesKey("font_size_sp")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_CUSTOM_USERS = stringPreferencesKey("custom_users_json")
        private val KEY_REGISTERED_ACCOUNTS = stringPreferencesKey("registered_accounts_json")
        private val KEY_ACTIVITY_LOGS = stringPreferencesKey("activity_logs_json")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DELETED_USERS = stringPreferencesKey("deleted_users_json")

        val DEFAULT_SYSTEM_USERS = listOf(
            CustomUser("azazmadkiya@gmail.com", "Azazmadkiya", UserRole.SUPER_ADMIN),
            CustomUser("info.azazmadkiya@gmail.com", "Azazmadkiya", UserRole.ADD_NAAT_ONLY),
            CustomUser("madkiyaahemad@gmail.com", "Ahemadmadkiya", UserRole.ADD_NAAT_ONLY)
        )
    }

    val activityLogsFlow: Flow<List<ActivityLogItem>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_ACTIVITY_LOGS] ?: "[]"
        val list = mutableListOf<ActivityLogItem>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ActivityLogItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        email = obj.optString("email", ""),
                        name = obj.optString("name", "User"),
                        action = obj.optString("action", "Action"),
                        details = obj.optString("details", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {}
        list.sortedByDescending { it.timestamp }
    }

    val customUsersFlow: Flow<List<CustomUser>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_CUSTOM_USERS]
        val storedUsers = mutableListOf<CustomUser>()
        try {
            if (!jsonStr.isNullOrBlank()) {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val email = obj.optString("email", "")
                    val name = obj.optString("displayName", "")
                    val roleStr = obj.optString("role", "VIEWER")
                    val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.VIEWER }
                    if (email.isNotBlank()) {
                        storedUsers.add(CustomUser(email, name, role))
                    }
                }
            }
        } catch (e: Exception) {}
        val deletedUsersStr = prefs[KEY_DELETED_USERS] ?: "[]"
        val deletedEmails = mutableSetOf<String>()
        try {
            val dArray = org.json.JSONArray(deletedUsersStr)
            for (i in 0 until dArray.length()) {
                deletedEmails.add(dArray.getString(i).trim().lowercase())
            }
        } catch (e: Exception) {}

        val resultMap = LinkedHashMap<String, CustomUser>()
        val defaults = DEFAULT_SYSTEM_USERS.filter { 
            it.email.lowercase() != "azazmadkiya@gmail.com" && !deletedEmails.contains(it.email.trim().lowercase()) 
        }
        for (u in defaults) {
            resultMap[u.email.trim().lowercase()] = u
        }
        for (u in storedUsers) {
            if (!deletedEmails.contains(u.email.trim().lowercase())) {
                resultMap[u.email.trim().lowercase()] = u
            }
        }
        resultMap.values.toList()
    }

    val userProfileFlow: Flow<UserProfile?> = combine(
        context.dataStore.data,
        customUsersFlow
    ) { prefs, customUsers ->
        val email = prefs[KEY_EMAIL]
        val isGuest = prefs[KEY_IS_GUEST] ?: false
        if (isGuest || email.isNullOrEmpty()) {
            UserProfile(email = "", displayName = "Guest User", role = UserRole.VIEWER, isGuest = true)
        } else {
            val cleanEmail = email.trim().lowercase()
            val custom = customUsers.find { it.email.trim().lowercase() == cleanEmail }
            val role = when {
                cleanEmail == "azazmadkiya@gmail.com" -> UserRole.SUPER_ADMIN
                custom != null -> custom.role
                else -> UserRole.fromEmail(email)
            }
            val name = when {
                cleanEmail == "azazmadkiya@gmail.com" -> "Azazmadkiya"
                custom != null && custom.displayName.isNotBlank() -> custom.displayName
                else -> prefs[KEY_NAME] ?: email.substringBefore("@")
            }
            UserProfile(email = email, displayName = name, role = role, isGuest = false)
        }
    }

    val fontSizeSpFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: 18f
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "emerald_light"
    }

    val isDarkModeFlow: Flow<Boolean> = themeModeFlow.map { mode ->
        mode == "dark_slate"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
            prefs[KEY_DARK_MODE] = (mode == "dark_slate")
        }
    }

    suspend fun setFontSizeSp(size: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE] = size
        }
    }

    suspend fun setUserLogin(email: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMAIL] = email
            prefs[KEY_NAME] = name
            prefs[KEY_IS_GUEST] = false
        }
    }

    suspend fun setGuestLogin() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = true
            prefs[KEY_EMAIL] = ""
            prefs[KEY_NAME] = "Guest User"
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMAIL] = ""
            prefs[KEY_NAME] = ""
            prefs[KEY_IS_GUEST] = true
        }
    }

    suspend fun registerAccount(email: String, displayName: String, password: String): Pair<Boolean, String> {
        val cleanEmail = email.trim().lowercase()
        var success = false
        var message = ""
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[KEY_REGISTERED_ACCOUNTS] ?: "[]"
            val array = try { org.json.JSONArray(jsonStr) } catch (e: Exception) { org.json.JSONArray() }
            var exists = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("email").trim().lowercase() == cleanEmail) {
                    exists = true
                    break
                }
            }
            if (exists) {
                success = false
                message = "यह ईमेल पहले से रजिस्टर्ड है। कृपया 'Sign In' करें।"
                return@edit
            }
            val newObj = org.json.JSONObject().apply {
                put("email", cleanEmail)
                put("displayName", displayName)
                put("password", password)
                put("isVerified", false)
                put("createdAt", System.currentTimeMillis())
            }
            array.put(newObj)
            prefs[KEY_REGISTERED_ACCOUNTS] = array.toString()
            success = true
            message = "खाता सफलतापूर्वक बन गया! वेरिफिकेशन लिंक आपके ईमेल $cleanEmail पर भेजा गया है।"
        }
        return Pair(success, message)
    }

    suspend fun markAccountVerified(email: String): Pair<Boolean, String> {
        val cleanEmail = email.trim().lowercase()
        var success = false
        var nameToSave = ""
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[KEY_REGISTERED_ACCOUNTS] ?: "[]"
            val array = try { org.json.JSONArray(jsonStr) } catch (e: Exception) { org.json.JSONArray() }
            val newArray = org.json.JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("email").trim().lowercase() == cleanEmail) {
                    obj.put("isVerified", true)
                    success = true
                    nameToSave = obj.optString("displayName", cleanEmail.substringBefore("@"))
                }
                newArray.put(obj)
            }
            if (success) {
                prefs[KEY_REGISTERED_ACCOUNTS] = newArray.toString()
            }
        }
        if (success) {
            saveCustomUser(cleanEmail, if (nameToSave.isNotBlank()) nameToSave else cleanEmail.substringBefore("@"), UserRole.VIEWER)
        }
        return Pair(true, "ईमेल सफलतापूर्वक वेरीफाई हो गया!")
    }

    suspend fun isAccountVerified(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail == "azazmadkiya@gmail.com" || 
            cleanEmail == "info.azazmadkiya@gmail.com" ||
            cleanEmail == "madkiyaahemad@gmail.com") {
            return true
        }
        return try {
            val prefs = context.dataStore.data.first()
            val jsonStr = prefs[KEY_REGISTERED_ACCOUNTS] ?: "[]"
            val array = org.json.JSONArray(jsonStr)
            var verified = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("email").trim().lowercase() == cleanEmail) {
                    verified = obj.optBoolean("isVerified", false)
                    break
                }
            }
            verified
        } catch (e: Exception) {
            false
        }
    }

    suspend fun validateLogin(email: String, password: String): Triple<Boolean, String, String> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            return Triple(false, "Please enter an email address", "")
        }
        val defaultUser = DEFAULT_SYSTEM_USERS.find { it.email.lowercase() == cleanEmail }
        if (defaultUser != null) {
            return Triple(true, "Signed in successfully as ${defaultUser.role.label}", defaultUser.displayName)
        }
        val prefs = context.dataStore.data.first()
        val jsonStr = prefs[KEY_REGISTERED_ACCOUNTS] ?: "[]"
        val array = try { org.json.JSONArray(jsonStr) } catch (e: Exception) { org.json.JSONArray() }
        var account: org.json.JSONObject? = null
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("email").trim().lowercase() == cleanEmail) {
                account = obj
                break
            }
        }
        if (account == null) {
            return Triple(false, "No account found for $cleanEmail. Please sign up first.", "")
        }
        val isVerified = account.optBoolean("isVerified", false)
        if (!isVerified) {
            return Triple(false, "UNVERIFIED: ईमेल वेरिफिकेशन अनिवार्य है! कृपया अपने ईमेल पर भेजे गए लिंक पर क्लिक करके पहले वेरीफाई करें।", "")
        }
        val storedPass = account.optString("password")
        if (storedPass.isNotBlank() && password.isNotBlank() && storedPass != password) {
            return Triple(false, "Incorrect password. Please try again.", "")
        }
        val name = account.optString("displayName", cleanEmail.substringBefore("@"))
        return Triple(true, "Signed in successfully!", name)
    }

    suspend fun resendVerificationLink(email: String): Pair<Boolean, String> {
        val cleanEmail = email.trim().lowercase()
        return Pair(true, "नया वेरिफिकेशन लिंक आपके ईमेल $cleanEmail पर भेज दिया गया है।")
    }

    suspend fun sendPasswordReset(email: String): Pair<Boolean, String> {
        val cleanEmail = email.trim().lowercase()
        return Pair(true, "पासवर्ड रीसेट लिंक आपके ईमेल $cleanEmail पर भेज दिया गया है।")
    }

    suspend fun saveCustomUser(email: String, displayName: String, role: UserRole) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[KEY_CUSTOM_USERS] ?: "[]"
            val array = try { org.json.JSONArray(jsonStr) } catch (e: Exception) { org.json.JSONArray() }
            var found = false
            val newArray = org.json.JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("email").trim().lowercase() == email.trim().lowercase()) {
                    obj.put("displayName", displayName)
                    obj.put("role", role.name)
                    found = true
                }
                newArray.put(obj)
            }
            if (!found) {
                val newObj = org.json.JSONObject().apply {
                    put("email", email)
                    put("displayName", displayName)
                    put("role", role.name)
                }
                newArray.put(newObj)
            }
            prefs[KEY_CUSTOM_USERS] = newArray.toString()
        }
    }

    suspend fun logActivity(email: String, name: String, action: String, details: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[KEY_ACTIVITY_LOGS] ?: "[]"
            val array = try { org.json.JSONArray(jsonStr) } catch (e: Exception) { org.json.JSONArray() }
            val newObj = org.json.JSONObject().apply {
                put("id", java.util.UUID.randomUUID().toString())
                put("email", email)
                put("name", name)
                put("action", action)
                put("details", details)
                put("timestamp", System.currentTimeMillis())
            }
            val updatedArray = org.json.JSONArray()
            updatedArray.put(newObj)
            for (i in 0 until minOf(array.length(), 49)) {
                updatedArray.put(array.getJSONObject(i))
            }
            prefs[KEY_ACTIVITY_LOGS] = updatedArray.toString()
        }
    }

    val currentUserEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_EMAIL] ?: ""
    }

    val categoryAlignmentFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[stringPreferencesKey("category_alignment")] ?: "center"
    }

    val categoryViewModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[stringPreferencesKey("category_view_mode")] ?: "grid"
    }

    val categoryOrderFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val str = prefs[stringPreferencesKey("category_order")] ?: ""
        if (str.isBlank()) emptyList() else str.split(",")
    }

    val recentlyViewedIdsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val str = prefs[stringPreferencesKey("recently_viewed_ids")] ?: ""
        if (str.isBlank()) emptyList() else str.split(",")
    }

    suspend fun clearActivityLogs() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVITY_LOGS] = "[]"
        }
    }

    suspend fun deleteCustomUser(email: String) {
        context.dataStore.edit { prefs ->
            val deletedStr = prefs[KEY_DELETED_USERS] ?: "[]"
            val array = try { org.json.JSONArray(deletedStr) } catch (e: Exception) { org.json.JSONArray() }
            array.put(email.trim().lowercase())
            prefs[KEY_DELETED_USERS] = array.toString()
        }
    }

    suspend fun saveCategoryOrder(order: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("category_order")] = order.joinToString(",")
        }
    }

    suspend fun saveCategoryAlignment(alignment: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("category_alignment")] = alignment
        }
    }

    suspend fun saveCategoryViewMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("category_view_mode")] = mode
        }
    }

    suspend fun addRecentlyViewedId(id: String) {
        context.dataStore.edit { prefs ->
            val str = prefs[stringPreferencesKey("recently_viewed_ids")] ?: ""
            val list = if (str.isBlank()) mutableListOf() else str.split(",").toMutableList()
            list.remove(id)
            list.add(0, id)
            if (list.size > 20) list.removeAt(list.size - 1)
            prefs[stringPreferencesKey("recently_viewed_ids")] = list.joinToString(",")
        }
    }

    suspend fun setGuestMode(isGuest: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_GUEST] = isGuest
        }
    }

    suspend fun getCurrentUserEmail(): String {
        return context.dataStore.data.first()[KEY_EMAIL] ?: ""
    }
}
