package pl.edu.finanseosobiste.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var budgetDao: BudgetDao

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        budgetDao = database.budgetDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertCategoryAndExpenses_calculatesCorrectCategorySummary() = runBlocking {
        val categoryId = budgetDao.insertCategory(
            CategoryEntity(
                name = "Jedzenie",
                colorHex = "#4CAF50",
                monthlyLimit = 1000.0
            )
        )

        val now = System.currentTimeMillis()
        budgetDao.insertExpense(
            ExpenseEntity(
                title = "Zakupy 1",
                amount = 300.0,
                categoryId = categoryId,
                timestamp = now
            )
        )
        budgetDao.insertExpense(
            ExpenseEntity(
                title = "Zakupy 2",
                amount = 550.0,
                categoryId = categoryId,
                timestamp = now
            )
        )

        val summaries = budgetDao.getCategorySummariesSince(now - 60_000L)
        assertEquals(1, summaries.size)
        assertEquals("Jedzenie", summaries[0].categoryName)
        assertEquals(850.0, summaries[0].totalSpent, 0.001)
        assertEquals(1000.0, summaries[0].monthlyLimit, 0.001)
    }

    @Test
    fun repeatedPaymentConfirmationRecordsOnlyOneExpense() = runBlocking {
        val category = budgetDao.insertCategory(CategoryEntity(name="Bills", colorHex="#000000", monthlyLimit=500.0))
        budgetDao.addRecurring(RecurringPayment(title="Rent", amount=100.0, categoryId=category,
            nextDate="2026-11-01", frequency=1, anchorDay=1))
        val id = database.openHelper.readableDatabase.query("SELECT id FROM recurring_payments").use {
            it.moveToFirst(); it.getLong(0)
        }
        val payment = RecurringPayment(id, "Rent", 100.0, category, "2026-11-01", 1, 1)
        assertEquals(true, budgetDao.recordPayment(payment, "2026-12-01", 1000L))
        assertEquals(false, budgetDao.recordPayment(payment, "2026-12-01", 1000L))
        assertEquals(100.0, budgetDao.getTotalSpentSince(0), 0.001)
    }

    @Test
    fun missingRowsDoNotReportMoneyChanges() = runBlocking {
        assertEquals(0, budgetDao.contribute(999, 10.0))
        assertEquals(0, budgetDao.deleteExpenseById(999))
        assertEquals(0, budgetDao.updateExpense(ExpenseEntity(999, "Missing", 10.0, 999)))
    }
}
