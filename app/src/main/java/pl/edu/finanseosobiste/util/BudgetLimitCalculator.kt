package pl.edu.finanseosobiste.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class LimitStatus {
    SAFE,
    WARNING,
    EXCEEDED
}

data class LimitEvaluation(
    val spent: Double,
    val limit: Double,
    val percentage: Int,
    val remaining: Double,
    val status: LimitStatus
)

object BudgetLimitCalculator {

    @Volatile var currencyCode: String = "PLN"
    @Volatile var displayLocale: Locale = Locale.ENGLISH

    const val DEFAULT_WARNING_THRESHOLD = 80

    fun evaluateLimit(
        spent: Double,
        limit: Double,
        warningThresholdPercent: Int = DEFAULT_WARNING_THRESHOLD
    ): LimitEvaluation {
        require(spent.isFinite() && limit.isFinite())
        require(warningThresholdPercent in 0..100)
        val safeSpent = spent.coerceAtLeast(0.0)
        if (limit <= 0.0) {
            val status = if (safeSpent > 0.0) LimitStatus.EXCEEDED else LimitStatus.SAFE
            return LimitEvaluation(
                spent = safeSpent,
                limit = 0.0,
                percentage = if (safeSpent > 0.0) 100 else 0,
                remaining = -safeSpent,
                status = status
            )
        }

        val rawPercentage = ((safeSpent / limit) * 100.0).roundToInt()
        val remaining = limit - safeSpent

        val status = when {
            safeSpent >= limit -> LimitStatus.EXCEEDED
            safeSpent / limit * 100.0 >= warningThresholdPercent -> LimitStatus.WARNING
            else -> LimitStatus.SAFE
        }

        return LimitEvaluation(
            spent = safeSpent,
            limit = limit,
            percentage = rawPercentage,
            remaining = remaining,
            status = status
        )
    }

    fun validateExpenseInput(title: String, amountText: String): Double? {
        if (title.trim().isEmpty()) return null
        return try { Money.parse(amountText)/100.0 } catch (_: IllegalArgumentException) { null } catch (_: ArithmeticException) { null }
    }

    fun formatCurrency(amount: Double): String {
        val formatter=java.text.NumberFormat.getNumberInstance(displayLocale).apply { minimumFractionDigits=2; maximumFractionDigits=2 }
        return formatter.format(amount) + " " + currencyCode
    }

    fun formatDate(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", displayLocale)
        return sdf.format(Date(timestampMillis))
    }

    fun getStartOfCurrentMonthMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
