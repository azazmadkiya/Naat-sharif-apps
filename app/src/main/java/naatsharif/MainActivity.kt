package naatsharif

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import naatsharif.data.local.NaatEntity
import naatsharif.ui.screens.AdminPortalScreen
import naatsharif.ui.screens.AuthScreen
import naatsharif.ui.screens.CategoryNaatsScreen
import naatsharif.ui.screens.FavoritesScreen
import naatsharif.ui.screens.HomeScreen
import naatsharif.ui.screens.NaatDetailScreen
import naatsharif.ui.screens.SettingsScreen
import naatsharif.ui.screens.SplashScreen
import naatsharif.ui.theme.NaatSharifTheme
import naatsharif.ui.viewmodel.CategoryInfo
import naatsharif.ui.viewmodel.NaatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NaatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            Log.i("MainActivity", "FirebaseApp initialization handled safely: ${e.message}")
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            NaatSharifTheme(themeMode = themeMode) {
                NaatAppNavigation(viewModel = viewModel)
            }
        }
    }
}

object NavRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val CATEGORY = "category"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val ADMIN_PORTAL = "admin_portal"
    const val DETAIL = "detail"
}

@Composable
fun NaatAppNavigation(viewModel: NaatViewModel) {
    val navController = rememberNavController()
    var selectedNaatForDetail by remember { mutableStateOf<NaatEntity?>(null) }
    var selectedCategoryForDetail by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        composable(NavRoutes.SPLASH) {
            val userProfile by viewModel.userProfile.collectAsState()
            SplashScreen(
                onTimeout = {
                    val isLoggedIn = userProfile != null && !userProfile!!.isGuest
                    if (isLoggedIn) {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.AUTH) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavRoutes.AUTH) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                },
                onGuestSelected = {
                    viewModel.loginAsGuest()
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                },
                onLoginWithEmail = { email, password, callback ->
                    viewModel.loginWithEmail(email, password, callback)
                },
                onRegisterUser = { email, name, password, callback ->
                    viewModel.registerUser(email, name, password, callback)
                },
                onCheckEmailVerification = { email, password, callback ->
                    viewModel.checkEmailVerification(email, password, callback)
                },
                onResendVerificationLink = { email, password, callback ->
                    viewModel.resendVerificationLink(email, password, callback)
                },
                onRequestPasswordReset = { email, callback ->
                    viewModel.requestPasswordReset(email, callback)
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNaatClick = { naat ->
                    selectedNaatForDetail = naat
                    navController.navigate(NavRoutes.DETAIL)
                },
                onCategoryClick = { category ->
                    selectedCategoryForDetail = category
                    navController.navigate(NavRoutes.CATEGORY)
                },
                onAdminPortalClick = { navController.navigate(NavRoutes.ADMIN_PORTAL) },
                onSuperAdminPortalClick = { navController.navigate(NavRoutes.ADMIN_PORTAL) },
                onLogoutClick = {
                    viewModel.logout()
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.CATEGORY) {
            selectedCategoryForDetail?.let { cat ->
                CategoryNaatsScreen(
                    category = cat,
                    viewModel = viewModel,
                    onNaatClick = { naat ->
                        selectedNaatForDetail = naat
                        navController.navigate(NavRoutes.DETAIL)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(NavRoutes.FAVORITES) {
            FavoritesScreen(
                viewModel = viewModel,
                onNaatClick = { naat ->
                    selectedNaatForDetail = naat
                    navController.navigate(NavRoutes.DETAIL)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAuth = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(NavRoutes.ADMIN_PORTAL)
                }
            )
        }

        composable(NavRoutes.ADMIN_PORTAL) {
            AdminPortalScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.DETAIL) {
            selectedNaatForDetail?.let { naat ->
                NaatDetailScreen(
                    naat = naat,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
