package pl.edu.finanseosobiste.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_config")
data class BudgetConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalMonthlyLimit: Double = 3500.0,
    val warningThresholdPercent: Int = 80
)
