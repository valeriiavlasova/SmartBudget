package pl.edu.finanseosobiste.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.edu.finanseosobiste.data.AppDatabase
import pl.edu.finanseosobiste.data.BudgetConfigEntity
import pl.edu.finanseosobiste.data.BudgetRepository
import pl.edu.finanseosobiste.data.CategoryEntity
import pl.edu.finanseosobiste.data.CategoryExpenseSummary
import pl.edu.finanseosobiste.data.ExpenseEntity
import pl.edu.finanseosobiste.data.ExpenseWithCategory
import pl.edu.finanseosobiste.notifications.NotificationHelper
import pl.edu.finanseosobiste.notifications.MoneyEvent
import pl.edu.finanseosobiste.util.BudgetLimitCalculator
import pl.edu.finanseosobiste.util.LimitStatus

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    var exportRows: List<pl.edu.finanseosobiste.data.ExpenseWithCategory> = emptyList()
    fun deleteWithUndo(expense: pl.edu.finanseosobiste.data.ExpenseWithCategory, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                val deleted = kotlinx.coroutines.withContext(Dispatchers.IO) { repository.deleteExpenseById(expense.id) }
                if (deleted > 0) {
                    notificationHelper.sendMoneyEvent(MoneyEvent.EXPENSE_DELETED, expense.title, expense.amount)
                    onDeleted()
                }
            }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { plannerMessage.value=pl.edu.finanseosobiste.R.string.save_error }
        }
    }
    fun restoreExpense(expense: pl.edu.finanseosobiste.data.ExpenseWithCategory) = plannerAction {
        repository.insertExpense(ExpenseEntity(expense.id,expense.title,expense.amount,expense.categoryId,expense.timestamp,expense.note,expense.accountId))
        notificationHelper.sendMoneyEvent(MoneyEvent.EXPENSE_RESTORED, expense.title, expense.amount)
    }
    private val plannerDao = AppDatabase.getInstance(application).budgetDao()
    val selectedMonth = androidx.lifecycle.MutableLiveData(java.time.YearMonth.now())
    val effectiveBudget = selectedMonth.switchMap { plannerDao.monthBudgetLive(it.toString()) }
    fun setFutureBudget(amount: Double) = plannerAction {
        require(amount.isFinite() && amount > 0)
        plannerDao.saveMonthBudget(pl.edu.finanseosobiste.data.MonthBudget(java.time.YearMonth.now().plusMonths(1).toString(), amount))
    }
    val history = selectedMonth.switchMap { month ->
        val zone = java.time.ZoneId.systemDefault()
        plannerDao.expensesInMonth(month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(), month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli())
    }
    val chartExpenses = selectedMonth.switchMap { month ->
        val zone = java.time.ZoneId.systemDefault()
        plannerDao.chartExpenses(month.minusMonths(5).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(), month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli())
    }
    val recurringPayments = plannerDao.recurringPayments()
    val savingsGoals = plannerDao.savingsGoals()
    val plannerMessage = androidx.lifecycle.MutableLiveData<Int?>()
    private fun plannerAction(action: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try { action(); plannerMessage.postValue(pl.edu.finanseosobiste.R.string.saved_success) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { plannerMessage.postValue(pl.edu.finanseosobiste.R.string.save_error) }
        }
    }
    fun addRecurring(payment: pl.edu.finanseosobiste.data.RecurringPayment) = plannerAction {
        require(payment.title.isNotBlank() && payment.amount.isFinite() && payment.amount>0 && payment.amount<=1e12)
        require(payment.frequency in 0..2 && payment.anchorDay in 1..31)
        java.time.LocalDate.parse(payment.nextDate)
        require(repository.getCategoryById(payment.categoryId)!=null)
        plannerDao.addRecurring(payment)
    }
    fun removeRecurring(id: Long) = plannerAction { plannerDao.deleteRecurring(id) }
    fun addGoal(goal: pl.edu.finanseosobiste.data.SavingsGoal) = plannerAction {
        require(goal.title.isNotBlank() && goal.target.isFinite() && goal.target>0 && goal.target<=1e12)
        require(goal.saved.isFinite() && goal.saved in 0.0..1e12)
        plannerDao.addGoal(goal)
    }
    fun removeGoal(id: Long) = plannerAction { plannerDao.deleteGoal(id) }
    fun contribute(id: Long, amount: Double) = plannerAction {
        require(amount.isFinite() && amount > 0)
        if (plannerDao.contribute(id, amount) > 0) {
            val title = savingsGoals.value?.firstOrNull { it.id == id }?.title
                ?: getApplication<Application>().getString(pl.edu.finanseosobiste.R.string.savings_notification_title)
            notificationHelper.sendMoneyEvent(MoneyEvent.SAVINGS_ADDED, title, amount)
        }
    }
    fun pay(payment: pl.edu.finanseosobiste.data.RecurringPayment) = plannerAction {
        val date = java.time.LocalDate.parse(payment.nextDate)
        require(!date.isAfter(java.time.LocalDate.now()))
        val next = pl.edu.finanseosobiste.util.PaymentSchedule.next(date, payment.frequency, payment.anchorDay)
        if (plannerDao.recordPayment(payment, next.toString(), date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())) {
            notificationHelper.sendMoneyEvent(MoneyEvent.PAYMENT_PAID, payment.title, payment.amount)
            checkLimitsAndNotify(payment.categoryId)
        }
    }
    fun editExpense(expense: ExpenseEntity) = plannerAction {
        require(expense.title.isNotBlank() && expense.amount.isFinite() && expense.amount > 0 && expense.amount<=1e12)
        require(expense.timestamp in 0..System.currentTimeMillis())
        require(AppDatabase.getInstance(getApplication()).financeDao().account(expense.accountId)!=null)
        if (plannerDao.updateExpense(expense) > 0) {
            notificationHelper.sendMoneyEvent(MoneyEvent.EXPENSE_EDITED, expense.title, expense.amount)
            checkLimitsAndNotify(expense.categoryId)
        }
    }
    private val repository: BudgetRepository
    private val notificationHelper = NotificationHelper(application)

    val allCategories: LiveData<List<CategoryEntity>>
    val currentMonthExpenses: LiveData<List<ExpenseWithCategory>>
    val categorySummaries: LiveData<List<CategoryExpenseSummary>>
    val budgetConfig: LiveData<BudgetConfigEntity?>

    init {
        val dao = AppDatabase.getInstance(application).budgetDao()
        repository = BudgetRepository(dao)
        allCategories = repository.allCategories
        currentMonthExpenses = repository.currentMonthExpenses
        categorySummaries = repository.currentMonthCategorySummaries
        budgetConfig = repository.budgetConfig
    }

    fun addExpense(title: String, amount: Double, categoryId: Long, note: String, timestamp: Long = System.currentTimeMillis(), accountId: Long = 1) {
        plannerAction {
            require(title.isNotBlank() && amount.isFinite() && amount>0 && amount<=1e12)
            require(timestamp in 0..System.currentTimeMillis())
            require(AppDatabase.getInstance(getApplication()).financeDao().account(accountId)!=null)
            val expense = ExpenseEntity(
                title = title.trim(),
                amount = amount,
                categoryId = categoryId,
                timestamp = timestamp,
                note = note.trim(), accountId = accountId
            )
            repository.insertExpense(expense)
            notificationHelper.sendMoneyEvent(MoneyEvent.EXPENSE_ADDED, expense.title, expense.amount)

            checkLimitsAndNotify(categoryId)
        }
    }

    fun deleteExpense(expenseId: Long) {
        plannerAction {
            repository.deleteExpenseById(expenseId)
        }
    }

    fun updateCategoryLimit(categoryId: Long, newLimit: Double) {
        plannerAction {
            require(newLimit.isFinite() && newLimit>0 && newLimit<=1e12)
            val category = repository.getCategoryById(categoryId) ?: return@plannerAction
            repository.updateCategory(category.copy(monthlyLimit = newLimit))
            checkLimitsAndNotify(categoryId)
        }
    }

    fun addCategory(name: String, monthlyLimit: Double, colorHex: String) {
        plannerAction {
            require(name.isNotBlank() && monthlyLimit.isFinite() && monthlyLimit>0 && monthlyLimit<=1e12)
            val newCategory = CategoryEntity(
                name = name.trim(),
                colorHex = colorHex,
                monthlyLimit = monthlyLimit
            )
            repository.insertCategory(newCategory)
        }
    }

    fun updateGlobalMonthlyLimit(newLimit: Double) {
        plannerAction {
            require(newLimit.isFinite() && newLimit>0 && newLimit<=1e12)
            repository.updateTotalMonthlyLimit(newLimit)
            checkGlobalLimitAndNotify()
        }
    }

    private suspend fun checkLimitsAndNotify(categoryId: Long) {
        val config = repository.getBudgetConfig()
        val category = repository.getCategoryById(categoryId)
        if (category != null && category.monthlyLimit > 0.0) {
            val categorySpent = repository.getTotalSpentForCategoryThisMonth(categoryId)
            val evaluation = BudgetLimitCalculator.evaluateLimit(
                spent = categorySpent,
                limit = category.monthlyLimit,
                warningThresholdPercent = config.warningThresholdPercent
            )
            if (evaluation.status == LimitStatus.WARNING || evaluation.status == LimitStatus.EXCEEDED) {
                notificationHelper.sendCategoryLimitAlert(
                    categoryId = category.id,
                    categoryName = category.name,
                    evaluation = evaluation
                )
            }
        }
        checkGlobalLimitAndNotify()
    }

    private suspend fun checkGlobalLimitAndNotify() {
        val config = repository.getBudgetConfig()
        if (config.totalMonthlyLimit > 0.0) {
            val totalSpent = repository.getTotalSpentThisMonth()
            val globalEvaluation = BudgetLimitCalculator.evaluateLimit(
                spent = totalSpent,
                limit = config.totalMonthlyLimit,
                warningThresholdPercent = config.warningThresholdPercent
            )
            if (globalEvaluation.status == LimitStatus.WARNING || globalEvaluation.status == LimitStatus.EXCEEDED) {
                notificationHelper.sendGlobalBudgetAlert(globalEvaluation)
            }
        }
    }
}
