package pl.edu.finanseosobiste.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.ui.MainActivity
import pl.edu.finanseosobiste.util.BudgetLimitCalculator
import pl.edu.finanseosobiste.util.LimitEvaluation
import pl.edu.finanseosobiste.util.LimitStatus

class NotificationHelper(private val baseContext: Context) {
    private val context get() = pl.edu.finanseosobiste.ui.AppSettings.localized(baseContext)

    companion object {
        const val CHANNEL_ID = "budget_limit_alerts_channel"
        const val MONEY_CHANNEL_ID = "money_actions_channel"
        private const val GLOBAL_NOTIFICATION_ID = 9001
    }

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_name)
            val descriptionText = context.getString(R.string.notif_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(NotificationChannel(
                MONEY_CHANNEL_ID, context.getString(R.string.money_channel_name), NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.money_channel_description) })
        }
    }

    fun sendCategoryLimitAlert(
        categoryId: Long,
        categoryName: String,
        evaluation: LimitEvaluation
    ) {
        if (!pl.edu.finanseosobiste.ui.AppSettings.preferences(context).getBoolean("notify_limits", true)) return
        if (evaluation.status == LimitStatus.SAFE) return
        if (!hasNotificationPermission()) return

        val spentFormatted = BudgetLimitCalculator.formatCurrency(evaluation.spent)
        val limitFormatted = BudgetLimitCalculator.formatCurrency(evaluation.limit)

        val (title, message) = when (evaluation.status) {
            LimitStatus.EXCEEDED -> {

                context.getString(R.string.notif_exceeded_title) to
                    context.getString(R.string.alert_category, pl.edu.finanseosobiste.ui.AppSettings.category(context, categoryName), evaluation.percentage, spentFormatted, limitFormatted)
            }
            LimitStatus.WARNING -> {
                context.getString(R.string.notif_warning_title) to
                    context.getString(R.string.alert_category, pl.edu.finanseosobiste.ui.AppSettings.category(context, categoryName), evaluation.percentage, spentFormatted, limitFormatted)
            }
            LimitStatus.SAFE -> return
        }

        showNotification(
            notificationId = (1000 + categoryId).toInt(),
            title = title,
            message = message
        )
    }

    fun sendGlobalBudgetAlert(evaluation: LimitEvaluation) {
        if (!pl.edu.finanseosobiste.ui.AppSettings.preferences(context).getBoolean("notify_limits", true)) return
        if (evaluation.status == LimitStatus.SAFE) return
        if (!hasNotificationPermission()) return

        val spentFormatted = BudgetLimitCalculator.formatCurrency(evaluation.spent)
        val limitFormatted = BudgetLimitCalculator.formatCurrency(evaluation.limit)

        val (title, message) = when (evaluation.status) {
            LimitStatus.EXCEEDED -> {

                context.getString(R.string.notif_exceeded_title) to
                    context.getString(R.string.alert_global, evaluation.percentage, spentFormatted, limitFormatted)
            }
            LimitStatus.WARNING -> {
                context.getString(R.string.notif_warning_title) to
                    context.getString(R.string.alert_global, evaluation.percentage, spentFormatted, limitFormatted)
            }
            LimitStatus.SAFE -> return
        }

        showNotification(
            notificationId = GLOBAL_NOTIFICATION_ID,
            title = title,
            message = message
        )
    }

    fun sendMoneyEvent(event: MoneyEvent, title: String, amount: Double) {
        if (!event.enabled(context) || !hasNotificationPermission()) return
        val message = context.getString(R.string.money_event_detail, title, BudgetLimitCalculator.formatCurrency(amount))
        // A separate tag keeps action notifications apart from budget alerts.
        showNotification(0, context.getString(event.label), message, MONEY_CHANNEL_ID, java.util.UUID.randomUUID().toString())
    }

    private fun showNotification(notificationId: Int, title: String, message: String,
        channelId: String = CHANNEL_ID, tag: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (channelId == CHANNEL_ID) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(tag, notificationId, builder.build())
        } catch (_: SecurityException) {
            // Brak uprawnień POST_NOTIFICATIONS w systemie Android 13+
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

