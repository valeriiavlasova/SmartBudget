package pl.edu.finanseosobiste.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        BudgetConfigEntity::class,
        RecurringPayment::class,
        SavingsGoal::class,
        MonthBudget::class, Account::class, CashEntry::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanse_osobiste_db"
                )
                    .addMigrations(object : androidx.room.migration.Migration(2, 3) {
                        override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS month_budgets (month TEXT NOT NULL PRIMARY KEY, amount REAL NOT NULL)") }
                    })
                    .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE IF NOT EXISTS recurring_payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, amount REAL NOT NULL, categoryId INTEGER NOT NULL, nextDate TEXT NOT NULL, frequency INTEGER NOT NULL, anchorDay INTEGER NOT NULL)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS savings_goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, target REAL NOT NULL, saved REAL NOT NULL)")
                        }
                    })
                    .addMigrations(FinanceMigration.FROM_3)
                    .addCallback(PrepopulateCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    internal class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL("INSERT OR IGNORE INTO accounts(id,name,openingMinor) VALUES(1,'Main account',0)")
            db.execSQL("INSERT INTO budget_config(id,totalMonthlyLimit,warningThresholdPercent) VALUES(1,3500,80)")
            val defaultCategories = listOf(
                CategoryEntity(1, "Jedzenie i zakupy", "#4CAF50", 1200.0),
                CategoryEntity(2, "Mieszkanie i rachunki", "#9C27B0", 1000.0),
                CategoryEntity(3, "Transport i paliwo", "#2196F3", 400.0),
                CategoryEntity(4, "Rozrywka i kultura", "#FF9800", 350.0),
                CategoryEntity(5, "Zdrowie i sport", "#E91E63", 300.0),
                CategoryEntity(6, "Inne wydatki", "#607D8B", 250.0)
            )
            defaultCategories.forEach {
                db.execSQL("INSERT INTO categories(id,name,colorHex,monthlyLimit) VALUES(?,?,?,?)",arrayOf(it.id,it.name,it.colorHex,it.monthlyLimit))
            }
            val now = System.currentTimeMillis()
            val sampleExpenses = listOf(
                ExpenseEntity(
                    title = "Zakupy spożywcze Biedronka",
                    amount = 245.50,
                    categoryId = 1,
                    timestamp = now - 86_400_000L * 3,
                    note = "Cotygodniowe zakupy"
                ),
                ExpenseEntity(
                    title = "Opłata za internet i telefon",
                    amount = 110.00,
                    categoryId = 2,
                    timestamp = now - 86_400_000L * 2,
                    note = "Faktura miesięczna"
                ),
                ExpenseEntity(
                    title = "Bilet miesięczny MPK Wrocław",
                    amount = 110.00,
                    categoryId = 3,
                    timestamp = now - 86_400_000L * 2,
                    note = "Komunikacja miejska"
                ),
                ExpenseEntity(
                    title = "Kino i kolacja ze znajomymi",
                    amount = 295.00,
                    categoryId = 4,
                    timestamp = now - 86_400_000L,
                    note = "Wyjście weekendowe"
                ),
                ExpenseEntity(
                    title = "Karnet na siłownię",
                    amount = 149.00,
                    categoryId = 5,
                    timestamp = now - 36_000_000L,
                    note = "Karnet studencki"
                )
            )
            sampleExpenses.forEach {
                db.execSQL("INSERT INTO expenses(title,amount,categoryId,timestamp,note,accountId) VALUES(?,?,?,?,?,?)",arrayOf(it.title,it.amount,it.categoryId,it.timestamp,it.note,it.accountId))
            }
        }
    }
}
