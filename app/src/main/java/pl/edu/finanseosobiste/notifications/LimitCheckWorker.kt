package pl.edu.finanseosobiste.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pl.edu.finanseosobiste.data.AppDatabase
import pl.edu.finanseosobiste.util.BudgetLimitCalculator
import pl.edu.finanseosobiste.util.LimitStatus

class LimitCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dao = AppDatabase.getInstance(applicationContext).budgetDao()
            val notificationHelper = NotificationHelper(applicationContext)
            val startOfMonth = BudgetLimitCalculator.getStartOfCurrentMonthMillis()
            val config = pl.edu.finanseosobiste.data.BudgetRepository(dao).getBudgetConfig()
            val threshold = config.warningThresholdPercent

            val summaries = dao.getCategorySummariesSince(startOfMonth)
            for (summary in summaries) {
                val eval = BudgetLimitCalculator.evaluateLimit(
                    spent = summary.totalSpent,
                    limit = summary.monthlyLimit,
                    warningThresholdPercent = threshold
                )
                if (eval.status == LimitStatus.WARNING || eval.status == LimitStatus.EXCEEDED) {
                    notificationHelper.sendCategoryLimitAlert(
                        categoryId = summary.categoryId,
                        categoryName = summary.categoryName,
                        evaluation = eval
                    )
                }
            }

            if (config.totalMonthlyLimit > 0.0) {
                val totalSpent = dao.getTotalSpentSince(startOfMonth)
                val globalEval = BudgetLimitCalculator.evaluateLimit(
                    spent = totalSpent,
                    limit = config.totalMonthlyLimit,
                    warningThresholdPercent = threshold
                )
                if (globalEval.status == LimitStatus.WARNING || globalEval.status == LimitStatus.EXCEEDED) {
                    notificationHelper.sendGlobalBudgetAlert(globalEval)
                }
            }

            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
