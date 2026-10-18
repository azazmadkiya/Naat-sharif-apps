package naatsharif

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class NaatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            Log.i("NaatApplication", "Firebase initialization handled safely: ${e.message}")
        }
    }
}
