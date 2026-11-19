package pl.edu.finanseosobiste.data

import androidx.lifecycle.LiveData
import pl.edu.finanseosobiste.util.BudgetLimitCalculator

class BudgetRepository(private val budgetDao: BudgetDao) {

    private val currentMonthStartMillis: Long
        get() = BudgetLimitCalculator.getStartOfCurrentMonthMillis()

    val allCategories: LiveData<List<CategoryEntity>> =
        budgetDao.getAllCategoriesLive()

    val currentMonthExpenses: LiveData<List<ExpenseWithCategory>>
        get() = budgetDao.getExpensesWithCategorySinceLive(currentMonthStartMillis)

    val currentMonthCategorySummaries: LiveData<List<CategoryExpenseSummary>>
        get() = budgetDao.getCategorySummariesSinceLive(currentMonthStartMillis)

    val budgetConfig: LiveData<BudgetConfigEntity?> =
        budgetDao.getBudgetConfigLive()

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        return budgetDao.insertExpense(expense)
    }

    suspend fun deleteExpenseById(expenseId: Long): Int = budgetDao.deleteExpenseById(expenseId)

    suspend fun insertCategory(category: CategoryEntity): Long {
        return budgetDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        budgetDao.updateCategory(category)
    }

    suspend fun getCategoryById(categoryId: Long): CategoryEntity? {
        return budgetDao.getCategoryById(categoryId)
    }

    suspend fun getTotalSpentForCategoryThisMonth(categoryId: Long): Double {
        return budgetDao.getTotalSpentForCategorySince(categoryId, currentMonthStartMillis)
    }

    suspend fun getTotalSpentThisMonth(): Double {
        return budgetDao.getTotalSpentSince(currentMonthStartMillis)
    }

    suspend fun getBudgetConfig(): BudgetConfigEntity {
        val base = budgetDao.getBudgetConfig() ?: BudgetConfigEntity()
        return base.copy(totalMonthlyLimit = budgetDao.monthBudget(java.time.YearMonth.now().toString()) ?: base.totalMonthlyLimit)
    }

    suspend fun updateTotalMonthlyLimit(newLimit: Double) {

        budgetDao.saveMonthBudget(MonthBudget(java.time.YearMonth.now().toString(), newLimit))
    }
}
