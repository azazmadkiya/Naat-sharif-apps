package naatsharif.data.model

enum class UserRole(val label: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Admin"),
    CONTRIBUTOR("Contributor"),
    ADD_NAAT_ONLY("Add Naat Sharif Only"),
    VIEW_ONLY("Add View Only"),
    VIEWER("Viewer / Reader");

    fun canManageAll(): Boolean = this == SUPER_ADMIN
    fun canManageUsers(): Boolean = this == SUPER_ADMIN
    fun canManageCategories(): Boolean = this == SUPER_ADMIN
    fun canAddNaat(): Boolean = this == SUPER_ADMIN || this == ADMIN || this == CONTRIBUTOR || this == ADD_NAAT_ONLY
    fun canApprove(): Boolean = this == SUPER_ADMIN || this == ADMIN
    fun canDelete(): Boolean = this == SUPER_ADMIN || this == ADMIN
    fun canAccessAdminPortal(): Boolean = this == SUPER_ADMIN || this == ADMIN || this == CONTRIBUTOR || this == ADD_NAAT_ONLY

    companion object {
        fun fromEmail(email: String?): UserRole {
            if (email.isNullOrBlank()) return VIEWER
            val cleanEmail = email.trim().lowercase()
            return when {
                cleanEmail == "azazmadkiya@gmail.com" -> SUPER_ADMIN
                cleanEmail == "info.azazmadkiya@gmail.com" -> ADD_NAAT_ONLY
                cleanEmail == "madkiyaahemad@gmail.com" -> ADD_NAAT_ONLY
                else -> VIEWER
            }
        }
    }
}

data class UserProfile(
    val email: String,
    val displayName: String,
    val role: UserRole,
    val isGuest: Boolean = false
)
