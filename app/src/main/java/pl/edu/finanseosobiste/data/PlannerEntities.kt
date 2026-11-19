package pl.edu.finanseosobiste.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_payments")
data class RecurringPayment(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val amount: Double, val categoryId: Long,
    val nextDate: String, val frequency: Int, val anchorDay: Int)

@Entity(tableName = "savings_goals")
data class SavingsGoal(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val target: Double, val saved: Double = 0.0)
