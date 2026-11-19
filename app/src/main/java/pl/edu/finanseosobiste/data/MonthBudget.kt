package pl.edu.finanseosobiste.data
@androidx.room.Entity(tableName="month_budgets")
data class MonthBudget(@androidx.room.PrimaryKey val month: String, val amount: Double)
