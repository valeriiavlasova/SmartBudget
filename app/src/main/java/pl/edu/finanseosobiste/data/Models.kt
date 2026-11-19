package pl.edu.finanseosobiste.data

data class ExpenseWithCategory(
    val id: Long,
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val timestamp: Long,
    val note: String,
    val categoryName: String,
    val categoryColorHex: String,
    val accountId: Long = 1
)

data class CategoryExpenseSummary(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val monthlyLimit: Double,
    val totalSpent: Double
)
