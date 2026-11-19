package pl.edu.finanseosobiste.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BudgetDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveMonthBudget(rule: MonthBudget)
    @Query("SELECT amount FROM month_budgets WHERE month <= :month ORDER BY month DESC LIMIT 1") suspend fun monthBudget(month: String): Double?
    @Query("SELECT amount FROM month_budgets WHERE month <= :month ORDER BY month DESC LIMIT 1") fun monthBudgetLive(month: String): LiveData<Double?>
    @Query("SELECT e.id,e.title,e.amount,e.categoryId,e.timestamp,e.note,e.accountId,c.name AS categoryName,c.colorHex AS categoryColorHex FROM expenses e INNER JOIN categories c ON e.categoryId=c.id WHERE e.timestamp >= :start AND e.timestamp < :end ORDER BY e.timestamp DESC")
    fun expensesInMonth(start: Long, end: Long): LiveData<List<ExpenseWithCategory>>
    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp")
    fun chartExpenses(start: Long, end: Long): LiveData<List<ExpenseEntity>>

    @Update suspend fun updateExpense(expense: ExpenseEntity): Int
    @Query("SELECT * FROM recurring_payments ORDER BY nextDate, title") fun recurringPayments(): LiveData<List<RecurringPayment>>
    @Insert suspend fun addRecurring(payment: RecurringPayment)
    @Query("DELETE FROM recurring_payments WHERE id=:id") suspend fun deleteRecurring(id: Long)
    @Query("UPDATE recurring_payments SET nextDate=:next WHERE id=:id AND nextDate=:expected") suspend fun advancePayment(id: Long, expected: String, next: String): Int
    @androidx.room.Transaction
    suspend fun recordPayment(payment: RecurringPayment, next: String, timestamp: Long): Boolean {
        if (advancePayment(payment.id, payment.nextDate, next) == 1) {
            insertExpense(ExpenseEntity(title=payment.title, amount=payment.amount, categoryId=payment.categoryId, timestamp=timestamp))
            return true
        }
        return false
    }
    @Query("SELECT * FROM savings_goals ORDER BY id DESC") fun savingsGoals(): LiveData<List<SavingsGoal>>
    @Insert suspend fun addGoal(goal: SavingsGoal)
    @Query("UPDATE savings_goals SET saved=saved+:amount WHERE id=:id AND :amount>0 AND saved+:amount<=1000000000000") suspend fun contribute(id: Long, amount: Double): Int
    @Query("DELETE FROM savings_goals WHERE id=:id") suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesLive(): LiveData<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long): Int

    @Query(
        """
        SELECT e.id, e.title, e.amount, e.categoryId, e.timestamp, e.note, e.accountId,
               c.name AS categoryName, c.colorHex AS categoryColorHex
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.timestamp >= :startOfMonthMillis
        ORDER BY e.timestamp DESC
        """
    )
    fun getExpensesWithCategorySinceLive(startOfMonthMillis: Long): LiveData<List<ExpenseWithCategory>>

    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS categoryName,
               c.colorHex AS colorHex,
               c.monthlyLimit AS monthlyLimit,
               COALESCE(SUM(CASE WHEN e.timestamp >= :startOfMonthMillis THEN e.amount ELSE 0.0 END), 0.0) AS totalSpent
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId
        GROUP BY c.id
        ORDER BY totalSpent DESC, c.name ASC
        """
    )
    fun getCategorySummariesSinceLive(startOfMonthMillis: Long): LiveData<List<CategoryExpenseSummary>>

    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS categoryName,
               c.colorHex AS colorHex,
               c.monthlyLimit AS monthlyLimit,
               COALESCE(SUM(CASE WHEN e.timestamp >= :startOfMonthMillis THEN e.amount ELSE 0.0 END), 0.0) AS totalSpent
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId
        GROUP BY c.id
        """
    )
    suspend fun getCategorySummariesSince(startOfMonthMillis: Long): List<CategoryExpenseSummary>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE categoryId = :categoryId AND timestamp >= :startOfMonthMillis")
    suspend fun getTotalSpentForCategorySince(categoryId: Long, startOfMonthMillis: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE timestamp >= :startOfMonthMillis")
    suspend fun getTotalSpentSince(startOfMonthMillis: Long): Double

    @Query("SELECT * FROM budget_config WHERE id = 1 LIMIT 1")
    fun getBudgetConfigLive(): LiveData<BudgetConfigEntity?>

    @Query("SELECT * FROM budget_config WHERE id = 1 LIMIT 1")
    suspend fun getBudgetConfig(): BudgetConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBudgetConfig(config: BudgetConfigEntity)
}
