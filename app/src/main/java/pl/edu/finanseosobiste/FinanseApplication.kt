package pl.edu.finanseosobiste

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import pl.edu.finanseosobiste.notifications.LimitCheckWorker
import pl.edu.finanseosobiste.notifications.NotificationHelper
import java.util.concurrent.TimeUnit

class FinanseApplication : Application(), androidx.work.Configuration.Provider {

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        val prefs=pl.edu.finanseosobiste.ui.AppSettings.preferences(this)
        if (!prefs.getBoolean("fresh_mint_v1", false)) prefs.edit().putInt("theme", 0).putBoolean("dark", false).putBoolean("fresh_mint_v1", true).apply()
        if (!prefs.getBoolean("english_core_v1", false)) prefs.edit().putString("language", "en").putBoolean("english_core_v1", true).apply()
        pl.edu.finanseosobiste.ui.AppSettings.configureMoney(this)
        NotificationHelper(this).createNotificationChannel()
        schedulePeriodicLimitCheck()
    }

    private fun schedulePeriodicLimitCheck() {
        val workRequest = PeriodicWorkRequestBuilder<LimitCheckWorker>(
            12, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PeriodicBudgetLimitCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
