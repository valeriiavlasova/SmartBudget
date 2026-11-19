package pl.edu.finanseosobiste.util
import java.time.LocalDate
object PaymentSchedule {
    fun next(date: LocalDate, frequency: Int, anchorDay: Int): LocalDate {
        require(frequency in 0..2 && anchorDay in 1..31)
        if (frequency == 0) return date.plusWeeks(1)
        val month = if (frequency == 1) date.withDayOfMonth(1).plusMonths(1) else date.withDayOfMonth(1).plusYears(1)
        return month.withDayOfMonth(minOf(anchorDay, month.lengthOfMonth()))
    }
}
