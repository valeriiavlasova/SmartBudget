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
            DemoData.seed(db)
        }
    }
}
