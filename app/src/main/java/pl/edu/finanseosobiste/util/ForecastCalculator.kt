package pl.edu.finanseosobiste.util

data class ForecastPoint(val day: Int, val balance: Double, val spending: Double)

object ForecastCalculator {
    fun project(start: Double, income: Double, spending: Double, days: Int): List<ForecastPoint> {
        require(start.isFinite() && income.isFinite() && spending.isFinite())
        require(income >= 0 && spending >= 0 && days in 28..31)
        return (0..days).map { day ->
            val fraction = day.toDouble() / days
            ForecastPoint(day, start + (income - spending) * fraction, spending * fraction)
        }
    }
}
