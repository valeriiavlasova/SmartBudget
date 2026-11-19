package pl.edu.finanseosobiste.notifications

import android.content.Context
import pl.edu.finanseosobiste.R
import pl.edu.finanseosobiste.ui.AppSettings

enum class MoneyEvent(val label: Int) {
    EXPENSE_ADDED(R.string.event_expense_added),
    EXPENSE_EDITED(R.string.event_expense_edited),
    EXPENSE_DELETED(R.string.event_expense_deleted),
    EXPENSE_RESTORED(R.string.event_expense_restored),
    PAYMENT_PAID(R.string.event_payment_paid),
    SAVINGS_ADDED(R.string.event_savings_added),
    INCOME_RECORDED(R.string.event_income_recorded),
    TRANSFER_RECORDED(R.string.event_transfer_recorded);

    val preferenceKey get() = "notify_" + name.lowercase(java.util.Locale.ROOT)
    fun enabled(context: Context) = AppSettings.preferences(context).getBoolean(preferenceKey, true)
}
